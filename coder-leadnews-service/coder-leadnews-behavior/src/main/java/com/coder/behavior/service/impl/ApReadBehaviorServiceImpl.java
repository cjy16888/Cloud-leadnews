package com.coder.behavior.service.impl;

import com.alibaba.fastjson.JSON;
import com.coder.behavior.service.ApReadBehaviorService;
import com.coder.common.constants.HotArticleConstants;
import com.coder.common.redis.CacheService;
import com.coder.model.behavior.dtos.ReadBehaviorDto;
import com.coder.model.common.dtos.ResponseResult;
import com.coder.model.common.enums.AppHttpCodeEnum;
import com.coder.model.mess.UpdateArticleMess;
import com.coder.model.user.pojos.ApUser;
import com.coder.utils.thread.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class ApReadBehaviorServiceImpl implements ApReadBehaviorService {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @Override
    public ResponseResult readBehavior(ReadBehaviorDto dto) {
        //1.检查参数
        if(dto == null || dto.getArticleId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.是否登录
        ApUser user = AppThreadLocalUtil.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        //更新阅读次数
        String readBehaviorJson = (String) cacheService.hGet(dto.getArticleId().toString(), user.getId().toString());
        if(StringUtils.isNotBlank(readBehaviorJson)){
            ReadBehaviorDto readBehaviorDto = JSON.parseObject(readBehaviorJson, ReadBehaviorDto.class);
            dto.setCount((short)(readBehaviorDto.getCount()+dto.getCount()));
        }
        // 保存当前key
        log.info("保存当前key:{} {} {}", dto.getArticleId(), user.getId(), dto);
        cacheService.hPut("READ-BEHAVIOR-" + dto.getArticleId().toString(), user.getId().toString(), JSON.toJSONString(dto));

        //发送消息，数据聚合
        //用户阅读都是正向的操作，不需要减一的判断
        UpdateArticleMess mess = new UpdateArticleMess();
        mess.setArticleId(dto.getArticleId());
        //阅读数+1
        mess.setType(UpdateArticleMess.UpdateArticleType.VIEWS);
        mess.setAdd(1);
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC,JSON.toJSONString(mess));

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

    }
}
