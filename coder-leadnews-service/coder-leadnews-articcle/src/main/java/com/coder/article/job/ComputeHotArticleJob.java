package com.coder.article.job;

import com.coder.article.service.HotArticleService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ComputeHotArticleJob {

    @Autowired
    private HotArticleService hotArticleService;

    //任务调度中心：http://localhost:8080/xxl-job-admin
    //下面的文章热点分值计算任务，每天凌晨2点执行一次
    //也可以到任务调度中心，手动进行触发执行
    @XxlJob("computeHotArticleJob")
    public void handle(){
        log.info("热文章分值计算调度任务开始执行...");
        hotArticleService.computeHotArticle();
        log.info("热文章分值计算调度任务结束...");

    }
}
