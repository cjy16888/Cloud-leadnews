package com.heima.search.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.service.ApAssociateWordsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/associate")
//什么时候会触发这个方法呢？当用户在搜索框中输入关键词的时候，就会触发这个方法
public class ApAssociateWordsController {

    @Autowired
    private ApAssociateWordsService apAssociateWordsService;

    // 搜索联想词，根据用户输入的关键词，模糊查询联想词
    @PostMapping("/search")
    public ResponseResult search(@RequestBody UserSearchDto dto){
        return apAssociateWordsService.search(dto);
    }
}
