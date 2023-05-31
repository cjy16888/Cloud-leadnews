package com.heima.common.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.heima.model.common.annotation.IdEncrypt;

import java.util.ArrayList;
import java.util.List;

public class ConfusionSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> newWriter = new ArrayList<>();
        for(BeanPropertyWriter writer : beanProperties){
            String name = writer.getType().getTypeName();
            //如果属性名是id，或者属性上有IdEncrypt注解，就进行加密
            //也就是进行序列化，转化为 string 对象，避免精度的丢失
            if(null == writer.getAnnotation(IdEncrypt.class) && !writer.getName().equalsIgnoreCase("id")){
                newWriter.add(writer);
            } else {
                writer.assignSerializer(new ConfusionSerializer());
                newWriter.add(writer);
            }
        }
        return newWriter;
    }
}
