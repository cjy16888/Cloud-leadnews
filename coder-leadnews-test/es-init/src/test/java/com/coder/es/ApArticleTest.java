package com.coder.es;

import com.alibaba.fastjson.JSON;
import com.coder.es.mapper.ApArticleMapper;
import com.coder.es.pojo.SearchArticleVo;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;


@SpringBootTest
@RunWith(SpringRunner.class)
public class ApArticleTest {

    @Autowired
    private ApArticleMapper apArticleMapper;

    //注入es客户端。用于操作es 进行数据导入
    @Autowired
    private RestHighLevelClient restHighLevelClient;


    /**
     * 注意：数据量的导入，如果数据量过大，需要分页导入
     * @throws Exception
     */
    @Test
    public void init() throws Exception {
        //如果数据量过大，需要分页导入
        //下面是代码实现
        //1.查询总条数
        //2.计算总页数
        //3.循环每页数据，每页1000条数据
        //4.批量导入到es索引库

        //1.查询所有符合条件的文章数据
        List<SearchArticleVo> searchArticleVos = apArticleMapper.loadArticleList();

        //2.批量导入到es索引库
        //2.1 创建批量导入对象  BulkRequest bulkRequest = new BulkRequest("索引库名称");
        //bulkRequest的主要作用是：批量导入数据
        BulkRequest bulkRequest = new BulkRequest("app_info_article");

        //2.2 循环遍历所有的文章数据，将每一条数据封装到IndexRequest对象中
        for (SearchArticleVo searchArticleVo : searchArticleVos) {
            //indexRequest的主要作用是：封装每一条数据
            //id：es索引库中的id，这里使用文章id
            //source：封装的数据，这里使用json格式
            IndexRequest indexRequest = new IndexRequest().id(searchArticleVo.getId().toString())
                    .source(JSON.toJSONString(searchArticleVo), XContentType.JSON);

            //批量添加数据，将每一条数据添加到bulkRequest对象中
            bulkRequest.add(indexRequest);
        }

        //2.3 执行批量导入操作
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

}