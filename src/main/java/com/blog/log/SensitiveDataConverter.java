package com.blog.log;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * 日志脱敏转换器
 *
 * 【企业级考点】
 * 1. 替换 Logback 默认的 %msg 转换器，在日志输出前自动清理敏感信息
 * 2. 防止手机号、邮箱、密码哈希等明文出现在日志文件中
 * 3. 对业务代码完全透明——不需要在每个 log.info() 里手动脱敏
 *
 * 配置方式：在 logback-spring.xml 中用 %dmsg 替代 %msg
 */
public class SensitiveDataConverter extends MessageConverter {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(\\w)[\\w.]*@(\\w+\\.\\w+)");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(password|密码|token|secret|refreshToken|accessToken)\\s*[:=]\\s*[\"']?([^\"'\\s,}]+)[\"']?",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String convert(ILoggingEvent event) {
        String msg = super.convert(event);
        if (msg == null) return null;
        return desensitize(msg);
    }

    /**
     * 对日志消息中的敏感信息进行脱敏
     */
    public static String desensitize(String message) {
        if (message == null || message.isEmpty()) return message;
        String result = message;
        result = desensitizePhone(result);
        result = desensitizeEmail(result);
        result = desensitizePassword(result);
        return result;
    }

    /**
     * 手机号脱敏：13800138000 → 138****8000
     */
    private static String desensitizePhone(String msg) {
        return PHONE_PATTERN.matcher(msg).replaceAll("$1****$2");
    }

    /**
     * 邮箱脱敏：zhangsan@gmail.com → z***@gmail.com
     */
    private static String desensitizeEmail(String msg) {
        return EMAIL_PATTERN.matcher(msg).replaceAll("$1***@$2");
    }

    /**
     * 密码/token 脱敏：password=123456 → password=******
     */
    private static String desensitizePassword(String msg) {
        return PASSWORD_PATTERN.matcher(msg).replaceAll("$1: ******");
    }
}
