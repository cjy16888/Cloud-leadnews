package com.coder.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.coder.file.service.FileStorageService;
import com.coder.model.common.dtos.PageResponseResult;
import com.coder.model.common.dtos.ResponseResult;
import com.coder.model.common.enums.AppHttpCodeEnum;
import com.coder.model.wemedia.dtos.WmMaterialDto;
import com.coder.model.wemedia.pojos.WmMaterial;
import com.coder.utils.thread.WmThreadLocalUtil;
import com.coder.wemedia.mapper.WmMaterialMapper;
import com.coder.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;


@Slf4j
@Service
@Transactional
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageService;


    /**
     * 图片上传
     * // TODO: 2023/5/10 待解决，13M图片太大 上传失败
     * @param multipartFile
     * @return
     */
    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {

        //1.检查参数
        if(multipartFile == null || multipartFile.getSize() == 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.上传图片到minIO中
        //随机名字，避免名字重复，覆盖
        String fileName = UUID.randomUUID().toString().replace("-", "");
        //aa.jpg
        String originalFilename = multipartFile.getOriginalFilename();
        //jpg后缀
        String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileId = null;
        try {
            //上传图片到minIO中
            fileId = fileStorageService.uploadImgFile("", fileName + postfix, multipartFile.getInputStream());
            log.info("上传图片到MinIO中，fileId:{}",fileId);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("WmMaterialServiceImpl-上传文件失败");
        }

        //3.保存到数据库中
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(WmThreadLocalUtil.getUser().getId());
        wmMaterial.setUrl(fileId);
        wmMaterial.setIsCollection((short)0);
        wmMaterial.setType((short)0);
        wmMaterial.setCreatedTime(new Date());
        save(wmMaterial);

        //4.返回结果
        return ResponseResult.okResult(wmMaterial);
    }

    /**
     * 素材列表查询
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmMaterialDto dto) {

        //1.检查参数
        dto.checkParam();

        //2.分页查询
        IPage page = new Page(dto.getPage(),dto.getSize());
        //条件查询, 添加全部的条件，然后再根据条件查询
        LambdaQueryWrapper<WmMaterial> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //是否收藏
        if(dto.getIsCollection() != null && dto.getIsCollection() == 1){
            lambdaQueryWrapper.eq(WmMaterial::getIsCollection,dto.getIsCollection());
        }

        //按照用户查询 每个用户只能看到自己的素材
        lambdaQueryWrapper.eq(WmMaterial::getUserId,WmThreadLocalUtil.getUser().getId());

        //按照时间倒序
        lambdaQueryWrapper.orderByDesc(WmMaterial::getCreatedTime);

        //分页查询，按照条件查询进行分页
        //这是mybatis-plus的方法
        page = page(page,lambdaQueryWrapper);

        //3.结果返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        //设置数据，getRecords()是mybatis-plus的方法，得到的是分页的数据
        responseResult.setData(page.getRecords());
        return responseResult;
    }
}
