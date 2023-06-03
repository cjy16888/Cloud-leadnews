package com.coder.search.service.impl;

import com.coder.model.common.dtos.ResponseResult;
import com.coder.model.common.enums.AppHttpCodeEnum;
import com.coder.model.search.dtos.UserSearchDto;
import com.coder.search.pojos.ApAssociateWords;
import com.coder.search.service.ApAssociateWordsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ApAssociateWordsServiceImpl implements ApAssociateWordsService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 搜索联想词
     * @param dto
     * @return
     */
    @Override
    public ResponseResult search(UserSearchDto dto) {
        //1.检查参数
        if(StringUtils.isBlank(dto.getSearchWords())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.分页检查
        if(dto.getPageSize() > 20){
            dto.setPageSize(20);
        }

        //3.执行查询，模糊查询
        //sql语句：select * from ap_associate_words where associate_words like '%xxx%'
        //这个正则表达式的意思是：.*?\\xxx.*  .*?表示任意字符，\\表示转义，xxx表示搜索的关键词
        Query query = Query.query(Criteria.where("associateWords").regex(".*?\\" + dto.getSearchWords() + ".*"));
        //只显示前20条
        query.limit(dto.getPageSize());
        List<ApAssociateWords> apAssociateWords = mongoTemplate.find(query, ApAssociateWords.class);
        return ResponseResult.okResult(apAssociateWords);
    }
}
