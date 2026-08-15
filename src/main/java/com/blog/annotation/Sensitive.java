package com.blog.annotation;

import com.blog.serializer.SensitiveSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.*;

/**
 * 数据脱敏注解
 *
 * 标注在 String 字段上，Jackson 序列化时自动脱敏。
 * 示例：@Sensitive(SensitiveType.PHONE) private String phone;
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {
    SensitiveType value();
}
