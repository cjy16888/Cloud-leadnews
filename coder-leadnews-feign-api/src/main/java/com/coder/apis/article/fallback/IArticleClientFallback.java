package com.coder.apis.article.fallback;

import com.coder.apis.article.IArticleClient;
import com.coder.model.article.dtos.ArticleDto;
import com.coder.model.common.dtos.ResponseResult;
import com.coder.model.common.enums.AppHttpCodeEnum;
import org.springframework.stereotype.Component;

@Component
public class IArticleClientFallback implements IArticleClient {
    @Override
    public ResponseResult saveArticle(ArticleDto dto) {
        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR,"降级处理：获取数据失败");
    }
}
