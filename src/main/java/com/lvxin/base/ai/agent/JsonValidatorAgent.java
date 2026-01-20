package com.lvxin.base.ai.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON验证智能体
 * 负责验证JSON的完整性和正确性
 *
 * @author lvxin
 */
@Slf4j
@Component
public class JsonValidatorAgent {

    /**
     * 验证JSON格式和结构
     *
     * @param jsonString JSON字符串
     * @return 验证结果
     */
    public ValidationResult validate(String jsonString) {
        ValidationResult result = new ValidationResult();

        if (jsonString == null || jsonString.trim().isEmpty()) {
            result.addError("JSON内容为空");
            return result;
        }

        // 1. 基本格式验证
        try {
            JSON.parse(jsonString);
            result.setValid(true);
        } catch (JSONException e) {
            result.addError("JSON格式错误：" + e.getMessage());
            return result;
        }

        // 2. 结构验证
        try {
            JSONObject jsonObject = JSON.parseObject(jsonString);

            // 验证必需字段
            if (!jsonObject.containsKey("title")) {
                result.addWarning("缺少title字段");
            }

            if (!jsonObject.containsKey("days")) {
                result.addError("缺少days字段");
            } else {
                // 验证days是否为数组
                Object days = jsonObject.get("days");
                if (days == null) {
                    result.addError("days字段为空");
                }
            }

            // 验证version和componentVersion
            if (!jsonObject.containsKey("version")) {
                result.addWarning("缺少version字段，将使用默认值1");
            }

            if (!jsonObject.containsKey("componentVersion")) {
                result.addWarning("缺少componentVersion字段，将使用默认值v1");
            }

        } catch (Exception e) {
            result.addError("结构验证失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * 验证结果
     */
    public static class ValidationResult {
        private boolean valid = false;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public boolean isValid() {
            return valid && errors.isEmpty();
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public String getErrorMessage() {
            if (errors.isEmpty()) {
                return null;
            }
            return String.join("; ", errors);
        }
    }
}
