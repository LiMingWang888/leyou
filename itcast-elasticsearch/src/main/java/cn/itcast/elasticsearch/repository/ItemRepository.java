package cn.itcast.elasticsearch.repository;

import cn.itcast.elasticsearch.pojo.Item;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author wlm
 * @date 2021/8/20 - 17:31
 */
public interface ItemRepository extends ElasticsearchRepository<Item,Long> {

}
