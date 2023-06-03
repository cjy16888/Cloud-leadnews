package com.coder.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.coder.common.constants.ScheduleConstants;
import com.coder.common.redis.CacheService;
import com.coder.model.schedule.dtos.Task;
import com.coder.model.schedule.pojos.Taskinfo;
import com.coder.model.schedule.pojos.TaskinfoLogs;
import com.coder.schedule.mapper.TaskinfoLogsMapper;
import com.coder.schedule.mapper.TaskinfoMapper;
import com.coder.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {

    /**
     * 添加延迟任务
     *
     * @param task
     * @return
     */
    @Override
    public long addTask(Task task) {
        //1.添加任务到数据库中
        boolean success = addTaskToDb(task);

        if (success) {
            //2.添加任务到redis
            addTaskToCache(task);
        }


        return task.getTaskId();
    }

    @Autowired
    private CacheService cacheService;

    /**
     * 把任务添加到redis中
     *
     * @param task
     */
    private void addTaskToCache(Task task) {
        //自定义key，如果是文章同步文章任务，key为：topic_article_1，如果是延时任务，key为：future_article_1
        String key =task.getTaskType() + "_" + task.getPriority();

        //获取5分钟之后的时间  毫秒值
        //获取当前时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        long nextScheduleTime = calendar.getTimeInMillis();

        //2.1 如果任务的执行时间小于等于当前时间，存入list
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            //存入list，key为：topic_article_1，供给消费者消费
            cacheService.lLeftPush(ScheduleConstants.TOPIC + key, JSON.toJSONString(task));
        } else if (task.getExecuteTime() <= nextScheduleTime) {
            //2.2 如果任务的执行时间大于当前时间 && 小于等于预设时间（未来5分钟） 存入zset中，定时刷新，时间一到，存入list
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task), task.getExecuteTime());
        }

        //如果是定时任务的话，并且时间超过了 5 分钟，那么不会存入到 redis 中，而是存入到了数据库中
    }

    @Autowired
    private TaskinfoMapper taskinfoMapper;

    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;

    /**
     * 添加任务到数据库中
     *
     * @param task
     * @return
     */
    private boolean addTaskToDb(Task task) {

        boolean flag = false;

        try {
            //保存任务表
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task, taskinfo);
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);

            //设置taskID
            task.setTaskId(taskinfo.getTaskId());

            //保存任务日志数据
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            BeanUtils.copyProperties(taskinfo, taskinfoLogs);
            taskinfoLogs.setVersion(1);
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
            taskinfoLogsMapper.insert(taskinfoLogs);

            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return flag;
    }


    /**
     * 取消任务
     *
     * @param taskId
     * @return
     */
    @Override
    public boolean cancelTask(long taskId) {

        boolean flag = false;

        //删除任务，更新任务日志
        Task task = updateDb(taskId, ScheduleConstants.CANCELLED);

        //删除redis的数据
        //因为 taskId 可能是不存在的
        if (task != null) {
            removeTaskFromCache(task);
            flag = true;
        }
        return flag;
    }

    /**
     * 删除redis中的数据
     *
     * @param task
     */
    private void removeTaskFromCache(Task task) {

        String key = task.getTaskType() + "_" + task.getPriority();
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            cacheService.lRemove(ScheduleConstants.TOPIC + key, 0, JSON.toJSONString(task));
        } else {
            cacheService.zRemove(ScheduleConstants.FUTURE + key, JSON.toJSONString(task));
        }

    }

    /**
     * 删除任务，更新任务日志
     *
     * @param taskId
     * @param status
     * @return
     */
    private Task updateDb(long taskId, int status) {

        Task task = null;

        try {
            //删除任务
            taskinfoMapper.deleteById(taskId);

            //更新任务日志
            //查询任务日志，当前任务的对应的日志
            TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
            //任务取消的标记
            taskinfoLogs.setStatus(status);
            taskinfoLogsMapper.updateById(taskinfoLogs);

            task = new Task();
            BeanUtils.copyProperties(taskinfoLogs, task);
            task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());
        } catch (Exception e) {
            log.error("task cancel exception taskId={}", taskId);
        }

        return task;
    }

    /**
     * 按照类型和优先级拉取 list 中的任务
     * 消费任务
     * @param type
     * @param priority
     * @return
     */
    @Override
    public Task poll(int type, int priority) {
        Task task = null;

        try {
            String key = type + "_" + priority;

            //从redis中拉取list中的数据  pop
            //返回值 string 是因为存入到redis的时候， task 任务对象被序列化了 toJsonString
            String task_json = cacheService.lRightPop(ScheduleConstants.TOPIC + key);

            //不为空，说明有任务
            if (StringUtils.isNotBlank(task_json)) {
                task = JSON.parseObject(task_json, Task.class);

                //修改数据库信息，表示任务已经被消费
                updateDb(task.getTaskId(), ScheduleConstants.EXECUTED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("poll task exception");
        }

        return task;
    }

    /**
     * 未来数据定时刷新
     * 每分钟执行一次，将未来5分钟之内的数据刷新到list中
     * cron表达式：秒 分 时 日 月 周 年
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void refresh() {

        //锁的时间 30s
        String token = cacheService.tryLock("FUTRUE_TASK_SYNC", 1000 * 30);

        //如果为空，说明获取锁失败
        if(StringUtils.isNotBlank(token)){
            log.info("未来数据定时刷新---定时任务");

            //获取所有未来数据的集合key
            Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
            for (String futureKey : futureKeys) {//future_100_50

                //获取当前数据的key  topic
                String topicKey = ScheduleConstants.TOPIC + futureKey.split(ScheduleConstants.FUTURE)[1];

                //按照key和分值查询符合条件的数据
                Set<String> tasks = cacheService.zRangeByScore(futureKey, 0, System.currentTimeMillis());

                //同步数据
                if (!tasks.isEmpty()) {
                    //futureKey 是用来删除 zset 中的数据的 key
                    //topicKey 是用来添加到 list 中的 key
                    //tasks 是要添加到 list 中的数据
                    cacheService.refreshWithPipeline(futureKey, topicKey, tasks);
                    log.info("成功的将" + futureKey + "刷新到了" + topicKey);
                }
            }
        }
    }

    /**
     * 从数据库中同步数据到缓存
     * 每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @PostConstruct //项目启动的时候执行一次
    public void reloadData() {
        clearCache();
        log.info("数据库数据同步到缓存");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);

        //查看小于未来5分钟的所有任务
        List<Taskinfo> allTasks = taskinfoMapper.selectList(Wrappers.<Taskinfo>lambdaQuery().lt(Taskinfo::getExecuteTime,calendar.getTime()));
        if(allTasks != null && allTasks.size() > 0){
            for (Taskinfo taskinfo : allTasks) {
                Task task = new Task();
                BeanUtils.copyProperties(taskinfo,task);
                task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                addTaskToCache(task);
            }
        }
    }

    /**
     * 清空缓存
     */
    private void clearCache(){
        // 删除缓存中未来数据集合和当前消费者队列的所有key，避免数据重复从数据库同步到缓存，数据库保存了所有的数据
        Set<String> futurekeys = cacheService.scan(ScheduleConstants.FUTURE + "*");// future_
        // 为什么删除 list 中key，list 中不是还有数据没有消费吗？因为 list 中的数据是从 zset 中同步过来的，所以 zset 中的数据是最新的
        // 但是如果，list 中的数据还没有被消费，就被这里删除了，那不是就丢失了吗？不会，因为 是五分钟刷新一次，定时也是五分钟一次，所以不会丢失
        Set<String> topickeys = cacheService.scan(ScheduleConstants.TOPIC + "*");// topic_
        cacheService.delete(futurekeys);
        cacheService.delete(topickeys);
    }
}
