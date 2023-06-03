package com.coder.model.article.vos;

import com.coder.model.article.pojos.ApArticle;
import lombok.Data;

@Data
public class HotArticleVo extends ApArticle {
    /**
     * 文章分值
     */
    private Integer score;
}
