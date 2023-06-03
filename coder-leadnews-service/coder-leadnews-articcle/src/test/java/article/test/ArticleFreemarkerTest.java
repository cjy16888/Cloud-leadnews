package article.test;


import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.coder.article.ArticleApplication;
import com.coder.article.mapper.ApArticleContentMapper;
import com.coder.article.mapper.ApArticleMapper;
import com.coder.file.service.FileStorageService;
import com.coder.model.article.pojos.ApArticle;
import com.coder.model.article.pojos.ApArticleContent;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = ArticleApplication.class)
@RunWith(SpringRunner.class)
public class ArticleFreemarkerTest {

    @Autowired
    private Configuration configuration;

    @Autowired
    private FileStorageService fileStorageService;


    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Test
    public void createStaticUrlTest() throws Exception {
        //1.获取文章内容
        ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, 1383827787629252610L));
        if(apArticleContent != null && StringUtils.isNotBlank(apArticleContent.getContent())){
            //2.文章内容通过freemarker生成html文件
            StringWriter out = new StringWriter();
            //获取模板
            Template template = configuration.getTemplate("article.ftl");

            //获取参数，返回给 ftl 模板
            Map<String, Object> params = new HashMap<>();
            //将文章内容转换成 JSONArray 对象，因为 String 对象在 ftl 模板中无法解析
            //content 是 ftl 模板中的参数，不能随意修改
            params.put("content", JSONArray.parseArray(apArticleContent.getContent()));

            //将参数传入模板中，生成 html 文件
            template.process(params, out);
            InputStream is = new ByteArrayInputStream(out.toString().getBytes());

            //3.把html文件上传到minio中
            String path = fileStorageService.uploadHtmlFile("", apArticleContent.getArticleId() + ".html", is);

            //4.修改ap_article表，保存static_url字段
            ApArticle article = new ApArticle();
            article.setId(apArticleContent.getArticleId());
            //设置静态化地址，这个地址是 minio 中的地址，前端访问的时候，需要通过 nginx 转发到 minio 中
            article.setStaticUrl(path);
            apArticleMapper.updateById(article);
        }
    }
}
