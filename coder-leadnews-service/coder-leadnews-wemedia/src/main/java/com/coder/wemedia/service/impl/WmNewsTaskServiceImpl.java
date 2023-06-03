package com.coder.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.coder.apis.schedule.IScheduleClient;
import com.coder.model.common.dtos.ResponseResult;
import com.coder.model.common.enums.TaskTypeEnum;
import com.coder.model.schedule.dtos.Task;
import com.coder.model.wemedia.pojos.WmNews;
import com.coder.utils.common.ProtostuffUtil;
import com.coder.wemedia.service.WmNewsAutoScanService;
import com.coder.wemedia.service.WmNewsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
@Slf4j
public class WmNewsTaskServiceImpl implements WmNewsTaskService {

    @Autowired
    private IScheduleClient scheduleClient;

    /**
     * 添加任务到延迟队列中
     * @param id          文章的id
     * @param publishTime 发布的时间  可以做为任务的执行时间
     */
    @Override
    @Async
    public void addNewsToTask(Integer id, Date publishTime) {

        log.info("添加任务到延迟服务中----begin");

        Task task = new Task();
        task.setExecuteTime(publishTime.getTime());
        task.setTaskType(TaskTypeEnum.NEWS_SCAN_TIME.getTaskType());
        task.setPriority(TaskTypeEnum.NEWS_SCAN_TIME.getPriority());
        WmNews wmNews = new WmNews();
        wmNews.setId(id);
        //把文章对象序列化成字节数组
        task.setParameters(ProtostuffUtil.serialize(wmNews));

        //添加任务到延迟队列中
        scheduleClient.addTask(task);

        log.info("添加任务到延迟服务中----end");

    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    /**
     * 消费任务，审核文章
     * fixedRate：每隔多少毫秒执行一次
     */
    @Scheduled(fixedRate = 1000)
    @Override
    public void scanNewsByTask() {

        log.info("消费任务，审核文章");

        ResponseResult responseResult = scheduleClient.poll(TaskTypeEnum.NEWS_SCAN_TIME.getTaskType(), TaskTypeEnum.NEWS_SCAN_TIME.getPriority());
        //如果有任务的话，进行拉取并且审核文章
        if(responseResult.getCode().equals(200) && responseResult.getData() != null){
            Task task = JSON.parseObject(JSON.toJSONString(responseResult.getData()), Task.class);
            WmNews wmNews = ProtostuffUtil.deserialize(task.getParameters(), WmNews.class);
            wmNewsAutoScanService.autoScanWmNews(wmNews.getId());
            System.out.println("成功消费任务：" + wmNews.getId());
        }
    }
}
