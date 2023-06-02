package com.heima.article.stream;

import com.alibaba.fastjson.JSON;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.mess.UpdateArticleMess;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class HotArticleStreamHandler {

    @Bean
    public KStream<String,String> kStream(StreamsBuilder streamsBuilder){
        //接收消息，消息的主题
        KStream<String,String> stream = streamsBuilder.stream(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC);
        //聚合流式处理
        stream.map((key,value)->{
            UpdateArticleMess mess = JSON.parseObject(value, UpdateArticleMess.class);
            //重置消息的key:1234343434   和  value: likes:1
            return new KeyValue<>(mess.getArticleId().toString(),mess.getType().name()+":"+mess.getAdd());
        })
                //按照文章id进行聚合
                .groupBy((key,value)->key)
                //时间窗口，10秒钟的时间窗口
                .windowedBy(TimeWindows.of(Duration.ofSeconds(10)))
                /**
                 * 自行的完成聚合的计算
                 */
                .aggregate(new Initializer<String>() {
                    /**
                     * 初始方法，返回值是消息的value
                     * @return
                     */
                    @Override
                    public String apply() {
                        return "COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0";
                    }
                    /**
                     * 真正的聚合操作，返回值是消息的value
                     */
                }, new Aggregator<String, String, String>() {
                    /**
                     *  真正的聚合操作
                     * @param key 文章的id
                     * @param value 文章的类型，收藏、评论、点赞、浏览
                     * @param aggValue 初始值，也是时间窗口内计算之后的值（第一次是 COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0）
                     * @return
                     */
                    @Override
                    public String apply(String key, String value, String aggValue) {
                        if(StringUtils.isBlank(value)){
                            return aggValue;
                        }
                        String[] aggAry = aggValue.split(",");
                        int col = 0,com = 0,lik = 0,vie = 0;

                        //将COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0进行拆分，单独计算每一个的值
                        //因为可能传送过来的是 COLLECTION:0,COMMENT:0,LIKES:10,VIEWS:0
                        //也就是这个瞬间，点赞的数量是10，其他的都是0，我们下面只需要计算点赞的数量即可
                        for (String agg : aggAry) {
                            String[] split = agg.split(":");
                            /**
                             * 获得初始值，也是时间窗口内计算之后的值
                             */
                            switch (UpdateArticleMess.UpdateArticleType.valueOf(split[0])){
                                case COLLECTION:
                                    col = Integer.parseInt(split[1]);
                                    break;
                                case COMMENT:
                                    com = Integer.parseInt(split[1]);
                                    break;
                                case LIKES:
                                    lik = Integer.parseInt(split[1]);
                                    break;
                                case VIEWS:
                                    vie = Integer.parseInt(split[1]);
                                    break;
                                default:
                            }
                        }
                        /**
                         * 上面的循环，只是为了获得初始值，这里就进行累加操作
                         * 累加操作
                         */
                        String[] valAry = value.split(":");
                        switch (UpdateArticleMess.UpdateArticleType.valueOf(valAry[0])){
                            case COLLECTION:
                                col += Integer.parseInt(valAry[1]);
                                break;
                            case COMMENT:
                                com += Integer.parseInt(valAry[1]);
                                break;
                            case LIKES:
                                lik += Integer.parseInt(valAry[1]);
                                break;
                            case VIEWS:
                                vie += Integer.parseInt(valAry[1]);
                                break;
                            default:
                        }

                        //返回值
                        String formatStr = String.format("COLLECTION:%d,COMMENT:%d,LIKES:%d,VIEWS:%d", col, com, lik, vie);
                        System.out.println("文章的id:"+key);
                        System.out.println("当前时间窗口内的消息处理结果："+formatStr);
                        return formatStr;
                    }
                }, Materialized.as("hot-atricle-stream-count-001"))
                .toStream()
                //将key和value进行拆分，使用key文章id作为消息的key，value还是上面的参数value，lambda表达式
                .map((key,value)->{
                    return new KeyValue<>(key.key().toString(),formatObj(key.key().toString(),value));
                })
                //发送消息
                .to(HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC);

        return stream;


    }

    /**
     * 格式化消息的value数据
     * 因为前面进行聚合操作是内部的，这里重新进行了一边格式化
     * @param articleId 文章id
     * @param value 文章的类型，收藏、评论、点赞、浏览，这个value是聚合之后的结果
     * @return
     */
    public String formatObj(String articleId,String value){
        ArticleVisitStreamMess mess = new ArticleVisitStreamMess();
        mess.setArticleId(Long.valueOf(articleId));
        //COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0
        String[] valAry = value.split(",");
        for (String val : valAry) {
            String[] split = val.split(":");
            switch (UpdateArticleMess.UpdateArticleType.valueOf(split[0])){
                case COLLECTION:
                    mess.setCollect(Integer.parseInt(split[1]));
                    break;
                case COMMENT:
                    mess.setComment(Integer.parseInt(split[1]));
                    break;
                case LIKES:
                    mess.setLike(Integer.parseInt(split[1]));
                    break;
                case VIEWS:
                    mess.setView(Integer.parseInt(split[1]));
                    break;
                default:
            }
        }

        log.info("聚合消息处理之后的结果为:{}",JSON.toJSONString(mess));
        return JSON.toJSONString(mess);
    }
}
