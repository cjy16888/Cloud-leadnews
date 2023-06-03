package com.coder.behavior.service;

import com.coder.model.behavior.dtos.UnLikesBehaviorDto;
import com.coder.model.common.dtos.ResponseResult;

/**
 * <p>
 * APP不喜欢行为表 服务类
 * </p>
 *
 * @author itcoder
 */
public interface ApUnlikesBehaviorService {

    /**
     * 不喜欢
     * @param dto
     * @return
     */
    public ResponseResult unLike(UnLikesBehaviorDto dto);

}