package com.coder.search.pojos;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 联想词表
 * </p>
 *
 * @author itcoder
 */
@Data
//就是指定MongoDB对应的表名，类似 Mysql 的@Table 表名
@Document("ap_associate_words")
//这个记录的是用户的搜索历史记录
public class ApAssociateWords implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    /**
     * 联想词
     */
    private String associateWords;

    /**
     * 创建时间
     */
    private Date createdTime;

}