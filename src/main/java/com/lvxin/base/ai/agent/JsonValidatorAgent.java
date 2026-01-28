package com.lvxin.base.ai.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON验证智能体
 * 负责验证JSON的完整性和正确性，并进行深度结构检查
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

        JSONObject root;
        // 1. 基本格式验证
        try {
            root = JSON.parseObject(jsonString);
            result.setValid(true);
        } catch (JSONException e) {
            result.addError("JSON格式错误：" + e.getMessage());
            result.setValid(false);
            return result;
        }

        // 2. 深度结构验证
        try {
            validateRoot(root, result);
        } catch (Exception e) {
            result.addError("结构验证异常：" + e.getMessage());
        }

        return result;
    }

    private void validateRoot(JSONObject root, ValidationResult result) {
        // 必需字段检查
        String[] requiredFields = {"title", "days", "version", "componentVersion"};
        for (String field : requiredFields) {
            if (!root.containsKey(field)) {
                result.addError("缺少顶层字段: " + field);
            }
        }

        // days 数组检查
        if (root.containsKey("days")) {
            Object daysObj = root.get("days");
            if (!(daysObj instanceof JSONArray)) {
                result.addError("days字段必须是数组");
            } else {
                JSONArray days = (JSONArray) daysObj;
                for (int i = 0; i < days.size(); i++) {
                    JSONObject day = days.getJSONObject(i);
                    validateDayBlock(day, i, result);
                }
            }
        }
    }

    private void validateDayBlock(JSONObject day, int index, ValidationResult result) {
        String path = "days[" + index + "]";
        if (!"DayBlock".equals(day.getString("type"))) {
            result.addError(path + " 类型错误，必须为 DayBlock");
        }

        if (!day.containsKey("id")) result.addError(path + " 缺少 id");
        
        // 检查 data
        if (!day.containsKey("data")) {
             result.addError(path + " 缺少 data 对象");
        } else {
            JSONObject data = day.getJSONObject("data");
            if (!data.containsKey("dayIndex")) result.addError(path + ".data 缺少 dayIndex");
        }

        // 检查 blocks
        if (!day.containsKey("blocks")) {
            result.addError(path + " 缺少 blocks 数组");
        } else {
            JSONArray blocks = day.getJSONArray("blocks");
            if (blocks == null) {
                result.addError(path + ".blocks 不能为 null");
            } else {
                for (int i = 0; i < blocks.size(); i++) {
                    validateContentBlock(blocks.getJSONObject(i), path + ".blocks[" + i + "]", result);
                }
            }
        }
    }

    private void validateContentBlock(JSONObject block, String path, ValidationResult result) {
        if (block == null) {
            result.addError(path + " 为 null");
            return;
        }

        String type = block.getString("type");
        if (type == null) {
            result.addError(path + " 缺少 type");
            return;
        }

        if (!block.containsKey("id")) result.addError(path + " 缺少 id");
        if (!block.containsKey("data")) result.addError(path + " 缺少 data");

        // SectionBlock 特有检查
        if ("SectionBlock".equals(type)) {
            if (!block.containsKey("blocks")) {
                result.addError(path + " (SectionBlock) 缺少 blocks 数组");
            } else {
                JSONArray subBlocks = block.getJSONArray("blocks");
                if (subBlocks == null) {
                    result.addError(path + ".blocks 不能为 null");
                } else {
                    for (int i = 0; i < subBlocks.size(); i++) {
                        // SectionBlock 只能包含 ItemBlock，不能再包含 SectionBlock (虽然技术上可以但业务上限制)
                        validateItemBlock(subBlocks.getJSONObject(i), path + ".blocks[" + i + "]", result);
                    }
                }
            }
        } else {
            // 其他 Block (ItemBlock) 不应包含 blocks 字段 (根据 iOS 解析报错 keyNotFound blocks)
            // 但如果 AI 生成了空的 blocks 数组，某些宽松的解析器可能忽略，但为了严格匹配 iOS 逻辑：
            // iOS 的 ItemBlock 定义中没有 blocks 字段。
            // 如果后端返回了 blocks 字段，前端 Codable 解析可能会忽略（如果是多余字段）
            // 但是！报错信息是 `keyNotFound(CodingKeys(stringValue: "blocks", ...)`
            // 这意味着前端在解析某个对象时 EXPECTS "blocks" 字段但没找到。
            // 让我们仔细看报错路径：
            // codingPath: days -> Index 0 -> blocks -> Index 0
            // 这对应的是 days[0].blocks[0]。
            // 让我们看原始数据：
            // days[0].blocks[0] 的 type 是 "SectionBlock"。
            // 它的 content 是: `{"type":"SectionBlock", ..., "items":[], ...}`
            // 等等！原始数据里没有 "blocks" 字段，而是有一个 "items": []。
            // 报错是因为 SectionBlock 必须要有 "blocks" 字段。
            // AI 生成的 JSON 里把 "blocks" 搞成了 "items" 或者完全漏了？
            // 看 Log 数据:
            // "blocks":[{"type":"SectionBlock", ... "items":[], ... }]
            // 确实，生成的 SectionBlock 里有一个 "items": []，但没有 "blocks"。
            // 这就是问题所在！
            
            if ("SectionBlock".equals(type) && !block.containsKey("blocks")) {
                // 这是一个致命错误，必须修复
                // 我们在 validate 里记录，之后 RepairStep 会修复
                result.addError(path + " (SectionBlock) 缺少 blocks 字段 (AI可能错误生成了 items 字段)");
            }
        }
    }

    private void validateItemBlock(JSONObject block, String path, ValidationResult result) {
        if (block == null) return;
        String type = block.getString("type");
        if ("SectionBlock".equals(type)) {
            result.addError(path + " 不能嵌套 SectionBlock");
        }
        // ItemBlock 主要是叶子节点，不需要额外检查 blocks
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
            valid = false;
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