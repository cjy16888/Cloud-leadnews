package com.coder.behavior.controller.v1;

import com.coder.behavior.service.ApUnlikesBehaviorService;
import com.coder.model.behavior.dtos.UnLikesBehaviorDto;
import com.coder.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/un_likes_behavior")
public class ApUnlikesBehaviorController {

    @Autowired
    private ApUnlikesBehaviorService apUnlikesBehaviorService;

    @PostMapping
    public ResponseResult unLike(@RequestBody UnLikesBehaviorDto dto){
        return apUnlikesBehaviorService.unLike(dto);
    }
}