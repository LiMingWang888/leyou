package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.item.pojo.*;
import com.leyou.search.GoodsRepository;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    public SearchResult search(SearchRequest request) {
        //??????????????????????????????
        if(StringUtils.isBlank(request.getKey())){
            return null;
        }

        //????????????????????????
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        //??????????????????
        //MatchQueryBuilder basicQuery = QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND);
        BoolQueryBuilder basicQuery = buildBasicQuery(request);
        queryBuilder.withQuery(basicQuery);

        // ???????????????????????????0??????
        queryBuilder.withPageable(PageRequest.of(request.getPage() - 1, request.getSize()));

        // ????????????????????????id subTitle skus
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "subTitle", "skus"}, null));

        // ??????????????????????????????
        String brandAggName = "brands";
        String categoryAggName = "categories";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));

        // ???????????????????????????
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());

        // ?????????????????????
        List<Brand> brands = getBrandAggResult(goodsPage.getAggregation(brandAggName));
        List<Map<String, Object>> categories = getCategoryAggResult(goodsPage.getAggregation(categoryAggName));

        List<Map<String, Object>> specs = null;
        if (!CollectionUtils.isEmpty(categories) && categories.size() == 1){
            specs = getParamAggName((Long) categories.get(0).get("id"), basicQuery);
        }

        // ?????????????????????
        return new SearchResult(goodsPage.getTotalElements(), goodsPage.getTotalPages(), goodsPage.getContent(), categories, brands, specs);
    }

    private BoolQueryBuilder buildBasicQuery(SearchRequest request) {
        //?????????bool??????
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //????????????????????????
        boolQueryBuilder.must(QueryBuilders.matchQuery("all",request.getKey()).operator(Operator.AND));
        //????????????
        for (Map.Entry<String, String> entry : request.getFilter().entrySet()) {
            String key = entry.getKey();
            if(StringUtils.equals(key, "??????")){
                key = "brandId";
            } else if (StringUtils.equals(key, "??????")) {
                key = "cid3";
            } else {
                key = "specs." + key + ".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery(key, entry.getValue()));
        }

        return boolQueryBuilder;
    }

    private List<Map<String, Object>> getParamAggName(Long id, QueryBuilder basicQuery) {
        //???????????????????????????
        List<SpecParam> params = this.specificationClient.queryParams(null, id, null, true);
        //??????????????????????????????
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // ????????????????????????
        queryBuilder.withQuery(basicQuery);
        // ???????????????????????????
        params.forEach(param -> {
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs." + param.getName() + ".keyword"));
        });
        // ?????????????????????
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{}, null));
        //????????????
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());
        //????????????????????????
        List<Map<String, Object>> paramMapList = new ArrayList<>();
        // ??????????????????????????????????????????Map<paramName, aggregation>
        Map<String, Aggregation> paramAggregationMap = goodsPage.getAggregations().asMap();
        // ?????????????????????????????????
        for (Map.Entry<String, Aggregation> entry : paramAggregationMap.entrySet()) {
            // ??????????????????????????????????????????????????????map
            Map<String,Object> map = new HashMap<>();
            // ??????k??????
            map.put("k", entry.getKey());

            // ??????????????????????????????
            StringTerms terms = (StringTerms)entry.getValue();
            List<Object> options = terms.getBuckets().stream().map(bucket -> bucket.getKeyAsString()).collect(Collectors.toList());
            // ??????options??????
            map.put("options", options);
            paramMapList.add(map);
        }
        return paramMapList;
    }

    private List<Map<String,Object>> getCategoryAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;

        return terms.getBuckets().stream().map(bucket -> {
            Map<String,Object> map = new HashMap<>();
            Long id = bucket.getKeyAsNumber().longValue();

            List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(id));
            map.put("id", id);
            map.put("name",names.get(0));
            return map;
        }).collect(Collectors.toList());
    }

    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        LongTerms terms = (LongTerms) aggregation;

        return terms.getBuckets().stream().map(bucket -> {
            Long id = bucket.getKeyAsNumber().longValue();

            return  this.brandClient.queryBrandById(id);
        }).collect(Collectors.toList());
    }

    public Goods buildGoods(Spu spu) throws IOException {
        Goods goods = new Goods();

        // ????????????id????????????
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // ??????cid1???cid2???cid3???????????????????????????
        List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        // ??????spuId???????????????sku
        List<Sku> skus = this.goodsClient.querySkusBySpuId(spu.getId());
        // ?????????????????????
        List<Long> prices = new ArrayList<>();
        // ?????????skuMapList????????????map???????????????sku???map??????key????????????id title image price
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

        // ??????spuDetail???????????????genericSpec SpecialSpec
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spu.getId());
        Map<Long, Object> genericSpecMap = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<Long, Object>>(){});
        Map<Long, List<Object>> specialSpecMap = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<Object>>>(){});

        // ?????????????????????????????????
        List<SpecParam> params = this.specificationClient.queryParams(null, spu.getCid3(), null, true);
        Map<String, Object> specs = new HashMap<>();
        params.forEach(param -> {
            // ?????????????????????????????????
            if (param.getGeneric()){
                // ?????????????????????
                String value = genericSpecMap.get(param.getId()).toString();
                // ???????????????????????????
                if (param.getNumeric()){
                    // ?????????????????????????????????????????????
                    value = chooseSegment(value, param);
                }
                specs.put(param.getName(), value);
            } else {
                // ?????????????????????
                List<Object> value = specialSpecMap.get(param.getId());
                specs.put(param.getName(), value);
            }
        });

        // ?????????????????????????????????goods?????????id subTitle brandId cid createTime
        BeanUtils.copyProperties(spu, goods);
        goods.setAll(spu.getTitle() + " " + brand.getName() + " " + StringUtils.join(names, " "));
        goods.setPrice(prices);
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        goods.setSpecs(specs);
        return goods;
    }


    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "??????";
        // ???????????????
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // ??????????????????
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // ????????????????????????
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "??????";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "??????";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public void saveIndex(Long spuId) throws IOException {
        Spu spu = this.goodsClient.querySpuById(spuId);
        Goods goods = this.buildGoods(spu);
        this.goodsRepository.save(goods);
    }

    public void deleteIndex(Long spuId) {
        this.goodsRepository.deleteById(spuId);
    }
}
