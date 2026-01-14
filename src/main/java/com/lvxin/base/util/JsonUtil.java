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
}


