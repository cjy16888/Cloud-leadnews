package com.coder.behavior.service;

import com.coder.model.behavior.dtos.ReadBehaviorDto;
import com.coder.model.common.dtos.ResponseResult;

public interface ApReadBehaviorService {

    /**
     * 保存阅读行为
     * @param dto
     * @return
     */
    public ResponseResult readBehavior(ReadBehaviorDto dto);
}
