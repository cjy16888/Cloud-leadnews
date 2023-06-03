package com.coder.behavior.service;

import com.coder.model.behavior.dtos.LikesBehaviorDto;
import com.coder.model.common.dtos.ResponseResult;

public interface ApLikesBehaviorService {

    /**
     * 存储喜欢数据
     * @param dto
     * @return
     */
    public ResponseResult like(LikesBehaviorDto dto);
}
