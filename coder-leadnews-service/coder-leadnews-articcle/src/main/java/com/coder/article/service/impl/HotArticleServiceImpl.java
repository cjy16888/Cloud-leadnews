package com.coder.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.coder.apis.wemedia.IWemediaClient;
import com.coder.article.mapper.ApArticleMapper;
import com.coder.article.service.HotArticleService;
import com.coder.common.constants.ArticleConstants;
import com.coder.common.redis.CacheService;
import com.coder.model.article.pojos.ApArticle;
import com.coder.model.article.vos.HotArticleVo;
import com.coder.model.common.dtos.ResponseResult;
import com.coder.model.wemedia.pojos.WmChannel;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class HotArticleServiceImpl implements HotArticleService {

    @Autowired
    private ApArticleMapper apArticleMapper;

    /**
     * 计算热点文章
     */
    @Override
    public void computeHotArticle() {
        //1.查询前5天的文章数据
        //当前的时减去5天
        Date dateParam = DateTime.now().minusDays(600).toDate();
        //sql语句：select * from ap_article where created_time >= dateParam
        List<ApArticle> apArticleList = apArticleMapper.findArticleListByLast5days(dateParam);

        //2.计算文章的分值
        List<HotArticleVo> hotArticleVoList = computeHotArticle(apArticleList);

        //3.为每个频道缓存30条分值较高的文章
        cacheTagToRedis(hotArticleVoList);

    }

    @Autowired
    private IWemediaClient wemediaClient;

    @Autowired
    private CacheService cacheService;

    /**
     * 为每个频道缓存30条分值较高的文章
     * @param hotArticleVoList
     */
    private void cacheTagToRedis(List<HotArticleVo> hotArticleVoList) {
        //每个频道缓存30条分值较高的文章
        ResponseResult responseResult = wemediaClient.getChannels();

        //获取所有的频道
        if(responseResult.getCode().equals(200)){
            //将频道转换为json字符串
            String channelJson = JSON.toJSONString(responseResult.getData());
            List<WmChannel> wmChannels = JSON.parseArray(channelJson, WmChannel.class);
            //检索出每个频道的文章
            if(wmChannels != null && wmChannels.size() > 0){
                for (WmChannel wmChannel : wmChannels) {
                    //相当于sql语句：select * from ap_article where channel_id = wmChannel.getId()
                    //过滤出每个频道的文章
                    List<HotArticleVo> hotArticleVos = hotArticleVoList.stream().filter(x -> x.getChannelId().equals(wmChannel.getId())).collect(Collectors.toList());
                    //给文章进行排序，取30条分值较高的文章存入redis  key：频道id   value：30条分值较高的文章
                    //每个频道文章的key：频道id
                    sortAndCache(hotArticleVos, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + wmChannel.getId());
                }
            }
        }


        //设置推荐数据
        //给文章进行排序，取30条分值较高的文章存入redis  key：频道id   value：30条分值较高的文章
        sortAndCache(hotArticleVoList, ArticleConstants.HOT_ARTICLE_FIRST_PAGE+ArticleConstants.DEFAULT_TAG);


    }

    /**
     * 排序并且缓存数据
     * @param hotArticleVos
     * @param key
     */
    private void sortAndCache(List<HotArticleVo> hotArticleVos, String key) {
        //相当于sql语句：select * from ap_article order by score desc
        hotArticleVos = hotArticleVos.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
        if (hotArticleVos.size() > 30) {
            hotArticleVos = hotArticleVos.subList(0, 30);
        }
        cacheService.set(key, JSON.toJSONString(hotArticleVos));
    }

    /**
     * 计算文章分值
     * @param apArticleList
     * @return
     */
    private List<HotArticleVo> computeHotArticle(List<ApArticle> apArticleList) {

        List<HotArticleVo> hotArticleVoList = new ArrayList<>();

        //计算文章的分值
        if(apArticleList != null && apArticleList.size() > 0){
            for (ApArticle apArticle : apArticleList) {
                HotArticleVo hot = new HotArticleVo();
                BeanUtils.copyProperties(apArticle,hot);
                //计算文章的分值
                Integer score = computeScore(apArticle);
                hot.setScore(score);
                //记录每一篇文章的分值
                hotArticleVoList.add(hot);
            }
        }
        return hotArticleVoList;
    }

    /**
     * 计算文章的具体分值
     * @param apArticle
     * @return
     */
    private Integer computeScore(ApArticle apArticle) {
        Integer scere = 0;
        //如果文章的点赞数不为空，那么分值加上点赞数*点赞权重
        if(apArticle.getLikes() != null){
            scere += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }
        //如果文章的阅读数不为空，那么分值加上阅读数*阅读权重
        if(apArticle.getViews() != null){
            scere += apArticle.getViews();
        }
        //如果文章的评论数不为空，那么分值加上评论数*评论权重
        if(apArticle.getComment() != null){
            scere += apArticle.getComment() * ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }
        //如果文章的收藏数不为空，那么分值加上收藏数*收藏权重
        if(apArticle.getCollection() != null){
            scere += apArticle.getCollection() * ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }

        return scere;
    }
}
