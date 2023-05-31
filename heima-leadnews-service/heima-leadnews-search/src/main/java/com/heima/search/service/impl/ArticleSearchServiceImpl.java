package com.heima.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.search.service.ApUserSearchService;
import com.heima.search.service.ArticleSearchService;
import com.heima.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ArticleSearchServiceImpl implements ArticleSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ApUserSearchService apUserSearchService;

    /**
     * es文章分页检索
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult search(UserSearchDto dto) throws IOException {

        //1.检查参数
        if(dto == null || StringUtils.isBlank(dto.getSearchWords())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //获取用户信息
        ApUser user = AppThreadLocalUtil.getUser();

        //异步调用 保存搜索记录
        //保存用户的搜索的历史记录，到 mongoDB 中
        //如果用户登录了，并且是第一页的话，才会保存，如果不是第一页说明是翻页，不需要保存（还是同一次搜索）
        if(user != null && dto.getFromIndex() == 0){
            apUserSearchService.insert(dto.getSearchWords(), user.getId());
        }

        //2.设置查询条件
        //通过索引名称创建查询请求对象，这样能够指定查询的索引库（类似Mysql有着多个数据库）
        //相当于 select * from app_info_article
        //这是一个请求，之后对es服务器的操作都是通过这个请求对象来进行的，类似于request
        SearchRequest searchRequest = new SearchRequest("app_info_article");
        //设置查询类型 QueryBuilders提供了很多静态方法，可以实现大部分查询条件的封装
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //布尔查询 用来封装多个查询条件 例如：关键字查询、时间范围查询、排序等
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //关键字的分词之后查询 title content 两个字段 or 查询 默认是and
        //整个查询的语句是：title like %关键字% or content like %关键字%
        QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(dto.getSearchWords()).field("title").field("content").defaultOperator(Operator.OR);
        //这是一个must查询 必须满足
        boolQueryBuilder.must(queryStringQueryBuilder);

        //查询小于mindate的数据
        //整个查询的语句是：publishTime < mindate
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("publishTime").lt(dto.getMinBehotTime().getTime());
        //这是一个filter查询 主要是用来过滤的 不会影响评分 也就是说不会影响查询结果的排序
        boolQueryBuilder.filter(rangeQueryBuilder);

        //分页查询 从第几条开始查询
        searchSourceBuilder.from(0);
        //查询多少条 也就是每页显示多少条
        searchSourceBuilder.size(dto.getPageSize());

        //按照发布时间倒序查询
        searchSourceBuilder.sort("publishTime", SortOrder.DESC);

        //设置高亮  title
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        //设置高亮字段 也就是说只有title字段会高亮显示
        highlightBuilder.field("title");
        //设置高亮样式 pre 就代表是在需要高亮文本之前，post是之后
        highlightBuilder.preTags("<font style='color: red; font-size: inherit;'>");
        highlightBuilder.postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);

        //设置查询条件 也就是将boolQueryBuilder设置到searchSourceBuilder中
        searchSourceBuilder.query(boolQueryBuilder);
        //将searchSourceBuilder设置到searchRequest中 因为searchRequest是用来执行查询的
        searchRequest.source(searchSourceBuilder);
        //执行查询
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);


        //3.结果封装返回
        List<Map> list = new ArrayList<>();

        //获取查询结果
        //getHits() 获取查询结果的hits数组，命中的数据
        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            //获取原始数据
            String json = hit.getSourceAsString();
            Map map = JSON.parseObject(json, Map.class);
            //处理高亮
            if(hit.getHighlightFields() != null && hit.getHighlightFields().size() > 0){
                //获取高亮标题 这个高亮的标题是携带html标签的（高亮）
                Text[] titles = hit.getHighlightFields().get("title").getFragments();
                //将高亮的标题拼接起来
                //join方法是将数组中的每个元素拼接起来
                //例如：String[] arr = {"a","b","c"};  String str = StringUtils.join(arr);  str = "abc"
                String title = StringUtils.join(titles);
                //高亮标题
                map.put("h_title",title);
            }else {
                //原始标题
                map.put("h_title",map.get("title"));
            }
            list.add(map);
        }

        return ResponseResult.okResult(list);
    }
}
