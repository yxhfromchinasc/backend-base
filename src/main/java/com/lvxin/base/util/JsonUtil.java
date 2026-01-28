package com.lvxin.base.util;

import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JSON 工具类：基于全局 ObjectMapper，提供静态方法
 */
@Component
public class JsonUtil {

    @Resource
    private ObjectMapper objectMapper;

    private static volatile ObjectMapper MAPPER;

    @PostConstruct
    public void init() {
        MAPPER = objectMapper;
    }

    /**
     * 将任意对象序列化为 JSON 字符串
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JSON序列化失败");
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型对象
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "JSON反序列化失败");
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定元素类型的 List
     */
    public static <T> List<T> toList(String json, Class<T> elementClass) {
        try {
            CollectionType listType = MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, elementClass);
            return MAPPER.readValue(json, listType);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "JSON列表反序列化失败");
        }
    }

    /**
     * 从包含 Markdown 代码块的文本中提取 JSON 内容
     * 规则：
     * 1. 优先提取 ```json ... ``` 或 ``` ... ``` 块中的内容
     * 2. 如果没有代码块，则提取第一个 { 和最后一个 } 之间的内容
     * 3. 如果都没有，返回原字符串
     */
    public static String extractJsonFromMarkdown(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // 1. 提取代码块
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "```(?:json)?\\s*([\\s\\S]*?)```",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.MULTILINE
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 2. 查找第一个 { 和最后一个 }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1).trim();
        }

        return text.trim();
    }
}


