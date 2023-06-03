package com.coder.model.article.dtos;

import com.coder.model.article.pojos.ApArticle;
import lombok.Data;

@Data
public class ArticleDto  extends ApArticle {

    /**
     * 文章内容
     */
    private String content;
}
