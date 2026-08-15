package com.blog.serializer;

import com.blog.annotation.Sensitive;
import com.blog.annotation.SensitiveType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * 数据脱敏序列化器
 *
 * 【企业级设计】
 * 1. 实现 ContextualSerializer，根据字段上的 @Sensitive 注解决定脱敏类型
 * 2. Jackson 序列化时自动触发，不需要手动调用
 * 3. 对 Controller / Service 完全透明——注解加在 VO 字段上即可
 */
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveType type;

    public SensitiveSerializer() {}

    public SensitiveSerializer(SensitiveType type) {
        this.type = type;
    }

    /**
     * 根据字段上的注解确定脱敏类型（Jackson 序列化前调用）
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            Sensitive annotation = property.getAnnotation(Sensitive.class);
            if (annotation != null) {
                return new SensitiveSerializer(annotation.value());
            }
        }
        return prov.findValueSerializer(String.class, property);
    }

    /**
     * 序列化时执行脱敏
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(mask(value, type));
    }

    /**
     * 根据类型执行脱敏
     */
    private String mask(String value, SensitiveType type) {
        if (value == null || value.isEmpty()) return value;
        return switch (type) {
            case PHONE -> value.substring(0, 3) + "****" + value.substring(value.length() - 4);
            case EMAIL -> {
                int at = value.indexOf('@');
                if (at <= 0) yield value;
                String name = value.substring(0, at);
                yield name.charAt(0) + "***" + value.substring(at);
            }
            case PASSWORD -> "******";
            case ID_CARD -> value.substring(0, 3) + "***********" + value.substring(value.length() - 4);
            case REAL_NAME -> {
                if (value.length() <= 1) yield value;
                if (value.length() == 2) yield value.charAt(0) + "*";
                yield value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
            }
        };
    }
}
