package com.coder.apis.article;

import com.coder.apis.article.fallback.IArticleClientFallback;
import com.coder.model.article.dtos.ArticleDto;
import com.coder.model.common.dtos.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

//这里的value是指定调用哪个服务
//fallback是指定调用失败的时候，调用哪个类中的方法,也就是我们平常所说的 降级熔断
@FeignClient(value = "leadnews-article", fallback = IArticleClientFallback.class)
@Component
public interface IArticleClient {

    //这里的PostMapping是指定调用哪个服务的哪个接口
    //其实有点类似 nginx的反向代理
    @PostMapping("/api/v1/article/save")
    //通过这个方法调用的时候，会自动调用leadnews-article微服务的/api/v1/article/save接口
    public ResponseResult saveArticle(@RequestBody ArticleDto dto);
}
