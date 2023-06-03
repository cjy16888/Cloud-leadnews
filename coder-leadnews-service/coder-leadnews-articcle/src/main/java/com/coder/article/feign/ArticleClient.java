package com.coder.article.feign;

import com.coder.apis.article.IArticleClient;
import com.coder.article.service.ApArticleService;
import com.coder.model.article.dtos.ArticleDto;
import com.coder.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ArticleClient implements IArticleClient {

    @Autowired
    private ApArticleService apArticleService;

    //这个路径是指定调用哪个服务的哪个接口，要和IArticleClient中的PostMapping一致
    @PostMapping("/api/v1/article/save")
    @Override
    public ResponseResult saveArticle(@RequestBody ArticleDto dto) {
        System.out.println("远程调用保存文章，调用成功");
        return apArticleService.saveArticle(dto);
    }
}
