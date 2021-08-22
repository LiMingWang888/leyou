package com.leyou.search.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.GoodsRepository;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;

import java.io.IOException;
import java.util.*;

/**
 * @author wlm
 * @date 2021/8/21 - 15:17
 */
@Service
public class SearchService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specificationClient;

    public static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private GoodsRepository goodsRepository;

    public PageResult<Goods> search(SearchRequest request) {

        if(StringUtils.isBlank(request.getKey())){
            return null;
        }

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.matchQuery("all",request.getKey()).operator(Operator.AND));
        queryBuilder.withPageable(PageRequest.of(request.getPage()-1, request.getSize()));
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","subTitle","skus"},null));

        Page<Goods> goodsPage = this.goodsRepository.search(queryBuilder.build());

        return new PageResult<>(goodsPage.getTotalElements(),goodsPage.getTotalPages(),goodsPage.getContent());

    }

    public Goods buildGoods(Spu spu) throws IOException {
        Goods goods = new Goods();

        // 根据品牌id查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // 根据cid1，cid2，cid3查询对应的分类名称
        List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        // 根据spuId查询所有的sku
        List<Sku> skus = this.goodsClient.querySkusBySpuId(spu.getId());
        // 初始化价格集合
        List<Long> prices = new ArrayList<>();
        // 初始化skuMapList，每一个map相当于一个sku，map中的key的取值：id title image price
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        skus.forEach(sku -> {
            prices.add(sku.getPrice());
            Map<String, Object> skuMap = new HashMap<>();
            skuMap.put("id", sku.getId());
            skuMap.put("title", sku.getTitle());
            skuMap.put("image", StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            skuMap.put("price", sku.getPrice());
            skuMapList.add(skuMap);
        });

        // 查询spuDetail，目的拿到genericSpec SpecialSpec
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spu.getId());
        Map<Long, Object> genericSpecMap = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<Long, Object>>(){});
        Map<Long, List<Object>> specialSpecMap = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<Object>>>(){});

        // 查询所有的搜索规格参数
        List<SpecParam> params = this.specificationClient.queryParams(null, spu.getCid3(), null, true);
        Map<String, Object> specs = new HashMap<>();
        params.forEach(param -> {
            // 判断是否通用的规格参数
            if (param.getGeneric()){
                // 通用的规格参数
                String value = genericSpecMap.get(param.getId()).toString();
                // 判断是否是数值类型
                if (param.getNumeric()){
                    // 如果是数值类型的参数，返回范围
                    value = chooseSegment(value, param);
                }
                specs.put(param.getName(), value);
            } else {
                // 特殊的规格参数
                List<Object> value = specialSpecMap.get(param.getId());
                specs.put(param.getName(), value);
            }
        });

        // 把简单的字段的值复制给goods对象：id subTitle brandId cid createTime
        BeanUtils.copyProperties(spu, goods);
        goods.setAll(spu.getTitle() + " " + brand.getName() + " " + StringUtils.join(names, " "));
        goods.setPrice(prices);
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        goods.setSpecs(specs);
        return goods;
    }


    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }
}
