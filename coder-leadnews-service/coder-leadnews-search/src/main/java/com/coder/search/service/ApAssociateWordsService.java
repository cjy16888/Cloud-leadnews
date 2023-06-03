package com.coder.search.service;

import com.coder.model.common.dtos.ResponseResult;
import com.coder.model.search.dtos.UserSearchDto;

public interface ApAssociateWordsService {

    /**
     * 搜索联想词
     * @param dto
     * @return
     */
    public ResponseResult search(UserSearchDto dto);
}
