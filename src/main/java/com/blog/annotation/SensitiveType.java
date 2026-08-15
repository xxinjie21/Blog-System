package com.blog.annotation;

/**
 * 数据脱敏注解
 *
 * 标注在 String 类型的字段上，Jackson 序列化时自动脱敏处理。
 * 示例：@Sensitive(SensitiveType.PHONE) private String phone;
 */
public enum SensitiveType {
    /** 手机号：138****8000 */
    PHONE,
    /** 邮箱：z***@gmail.com */
    EMAIL,
    /** 密码：返回 "******" */
    PASSWORD,
    /** 身份证：110***********1234 */
    ID_CARD,
    /** 真实姓名：张*三 */
    REAL_NAME
}
