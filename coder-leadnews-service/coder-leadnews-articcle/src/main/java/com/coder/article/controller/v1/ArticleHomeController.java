package com.coder.article.controller.v1;

import com.coder.article.service.ApArticleService;
import com.coder.common.constants.ArticleConstants;
import com.coder.model.article.dtos.ArticleHomeDto;
import com.coder.model.common.dtos.ResponseResult;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/article")
public class ArticleHomeController {

    @Autowired
    private ApArticleService apArticleService;

    /**
     * 加载首页
     * @param dto
     * @return
     */
    @PostMapping("/load")
    public ResponseResult load(@RequestBody ArticleHomeDto dto){
        //return apArticleService.load(dto, ArticleConstants.LOADTYPE_LOAD_MORE);
        //首页的话，需要加载缓存中的数据（热点文章）
        return apArticleService.load2(dto, ArticleConstants.LOADTYPE_LOAD_MORE,true);
    }

    /**
     * 加载更多
     * @param dto
     * @return
     */
    @PostMapping("/loadmore")
    public ResponseResult loadmore(@RequestBody ArticleHomeDto dto){
        return apArticleService.load(dto, ArticleConstants.LOADTYPE_LOAD_MORE);
    }

    /**
     * 加载最新
     * @param dto
     * @return
     */
    @PostMapping("/loadnew")
    public ResponseResult loadnew(@RequestBody ArticleHomeDto dto){
        return apArticleService.load(dto, ArticleConstants.LOADTYPE_LOAD_NEW);
    }
}
