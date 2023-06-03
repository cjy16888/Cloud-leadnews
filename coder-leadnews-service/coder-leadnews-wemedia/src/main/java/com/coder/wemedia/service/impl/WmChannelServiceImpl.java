package com.coder.wemedia.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coder.model.common.dtos.ResponseResult;
import com.coder.model.wemedia.pojos.WmChannel;
import com.coder.wemedia.mapper.WmChannelMapper;
import com.coder.wemedia.service.WmChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class WmChannelServiceImpl extends ServiceImpl<WmChannelMapper, WmChannel> implements WmChannelService {


    /**
     * 查询所有频道
     * @return
     */
    @Override
    public ResponseResult findAll() {
        //list()方法是mybatis-plus提供的方法，不需要写sql语句
        //list是查询所有数据，不带条件
        return ResponseResult.okResult(list());
    }
}