package com.lvxin.base.ai.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * 行程计划表 JSON Schema 工厂
 * 用于生成符合 Grok/OpenAI 结构化输出要求的 Schema
 *
 * Version 1.2: 严格对齐 iOS 端 Swift Codable 模型
 */
public class ItinerarySchemaFactory {

    public static JSONObject getItinerarySchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        
        JSONObject properties = new JSONObject();
        schema.put("properties", properties);
        
        // --- Itinerary (ItineraryModels.swift: struct Itinerary) ---
        properties.put("id", stringType());
        properties.put("version", integerType());
        properties.put("componentVersion", stringType());
        properties.put("title", stringType());
        properties.put("coverImageURL", nullableString()); // Deprecated legacy field
        properties.put("coverImageURLs", nullableArrayString()); // New field
        
        // Days Array
        JSONObject daysProperty = new JSONObject();
        daysProperty.put("type", "array");
        daysProperty.put("items", getDayBlockSchema());
        properties.put("days", daysProperty);

        // Required fields
        JSONArray required = new JSONArray();
        required.add("id");
        required.add("version");
        required.add("componentVersion");
        required.add("title");
        required.add("days");
        required.add("coverImageURL");
        required.add("coverImageURLs");
        schema.put("required", required);

        return schema;
    }

    private static JSONObject getDayBlockSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        
        JSONObject properties = new JSONObject();
        schema.put("properties", properties);
        
        // --- DayBlock (ItineraryModels.swift: struct DayBlock) ---
        properties.put("type", constString("DayBlock"));
        properties.put("id", stringType());
        
        // data: DayBlockData
        JSONObject dataProps = new JSONObject();
        dataProps.put("dayIndex", integerType());
        dataProps.put("date", nullableString());
        dataProps.put("title", stringType());
        dataProps.put("summary", nullableString());
        properties.put("data", objectType(dataProps));
        
        // blocks: [DayContentBlock]
        // DayContentBlock is enum: .sectionBlock | .itemBlock
        JSONObject blocksProperty = new JSONObject();
        blocksProperty.put("type", "array");
        blocksProperty.put("items", getDayContentBlockSchema()); // Section or Item
        properties.put("blocks", blocksProperty);

        JSONArray required = new JSONArray();
        required.add("type");
        required.add("id");
        required.add("data");
        required.add("blocks");
        schema.put("required", required);

        return schema;
    }
    
    // DayContentBlock: Can be SectionBlock OR ItemBlock
    private static JSONObject getDayContentBlockSchema() {
        // In strict JSON schema without definitions, we need a unified structure or "oneOf" logic simulated.
        // Since we are using "type" discriminator, we can define a super-object that contains fields for ALL block types.
        // This is the "Unified Schema" strategy.
        
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        
        JSONObject properties = new JSONObject();
        schema.put("properties", properties);
        
        // type
        JSONObject typeSchema = new JSONObject();
        typeSchema.put("type", "string");
        JSONArray enums = new JSONArray();
        enums.add("SectionBlock");
        enums.add("TransportBlock");
        enums.add("PlaceBlock");
        enums.add("FoodBlock");
        enums.add("AccommodationBlock");
        enums.add("NoteBlock");
        enums.add("CostSummaryBlock");
        typeSchema.put("enum", enums);
        properties.put("type", typeSchema);
        
        properties.put("id", stringType());
        
        // data: Unified Data Object covering ALL block data types
        properties.put("data", getUnifiedDataSchema());
        
        // blocks: Only for SectionBlock (contains ItemBlock)
        JSONObject subBlocks = new JSONObject();
        subBlocks.put("type", "array");
        subBlocks.put("items", getItemBlockSchema()); // Only Items inside Section
        properties.put("blocks", subBlocks);

        JSONArray required = new JSONArray();
        required.add("type");
        required.add("id");
        required.add("data");
        required.add("blocks"); // Required for strict mode, can be empty
        schema.put("required", required);

        return schema;
    }

    // ItemBlock: Leaf nodes (Transport, Place, Food, etc.)
    private static JSONObject getItemBlockSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        
        JSONObject properties = new JSONObject();
        schema.put("properties", properties);
        
        // type: No SectionBlock here
        JSONObject typeSchema = new JSONObject();
        typeSchema.put("type", "string");
        JSONArray enums = new JSONArray();
        enums.add("TransportBlock");
        enums.add("PlaceBlock");
        enums.add("FoodBlock");
        enums.add("AccommodationBlock");
        enums.add("NoteBlock");
        enums.add("CostSummaryBlock");
        typeSchema.put("enum", enums);
        properties.put("type", typeSchema);
        
        properties.put("id", stringType());
        properties.put("data", getUnifiedDataSchema());
        
        // blocks: Not present in ItemBlock structures in Swift (they are leaf nodes)
        // BUT for strict schema consistency if we reused the object, we might need it.
        // However, we are defining a separate ItemBlockSchema here.
        // Swift ItemBlock structs (e.g. TransportBlock) DO NOT have a 'blocks' field.
        // So we omit it here.
        
        JSONArray required = new JSONArray();
        required.add("type");
        required.add("id");
        required.add("data");
        schema.put("required", required);

        return schema;
    }

    private static JSONObject getUnifiedDataSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        
        JSONObject properties = new JSONObject();
        schema.put("properties", properties);
        
        // --- DayBlockData ---
        properties.put("dayIndex", nullableInteger());
        properties.put("date", nullableString());
        // properties.put("title", nullableString()); // Shared
        // properties.put("summary", nullableString()); // Shared

        // --- SectionBlockData ---
        properties.put("title", nullableString());
        properties.put("timeRange", nullableString());
        properties.put("summary", nullableString());

        // --- TransportBlockData ---
        properties.put("transportType", nullableString());
        properties.put("fromName", nullableString());
        properties.put("fromLocation", getLocationSchema());
        properties.put("toName", nullableString());
        properties.put("toLocation", getLocationSchema());
        properties.put("departureTime", nullableString());
        properties.put("arrivalTime", nullableString());
        properties.put("code", nullableString());
        properties.put("duration", nullableString());
        properties.put("cost", nullableNumber());
        // properties.put("note", nullableString()); // Shared
        properties.put("imageURLs", nullableArrayString());
        properties.put("externalLinks", getExternalLinkArraySchema());

        // --- PlaceBlockData ---
        properties.put("name", nullableString());
        // properties.put("type", nullableString()); // Shared (subtype)
        // properties.put("location", getLocationSchema()); // Shared
        properties.put("stayDuration", nullableString());
        properties.put("ticketInfo", nullableString());
        properties.put("bestTime", nullableString());
        properties.put("highlights", nullableArrayString());
        properties.put("tips", nullableString());
        
        // --- FoodBlockData ---
        properties.put("time", nullableString());
        properties.put("perCapita", nullableNumber());
        properties.put("mustTry", nullableArrayString());
        properties.put("rating", nullableNumber());
        properties.put("tags", nullableArrayString());
        
        // --- AccommodationBlockData ---
        properties.put("checkIn", nullableString());
        properties.put("checkOut", nullableString());
        properties.put("pricePerNight", nullableNumber());
        properties.put("totalPrice", nullableNumber());
        properties.put("roomType", nullableString());
        properties.put("contact", nullableString());
        
        // --- NoteBlockData ---
        // properties.put("title", nullableString()); // Shared
        properties.put("content", nullableString());
        properties.put("level", nullableString());
        
        // --- CostSummaryBlockData ---
        properties.put("scope", nullableString());
        properties.put("items", getCostItemArraySchema());
        properties.put("total", nullableNumber());
        properties.put("currency", nullableString());

        // --- Shared Fields ---
        properties.put("location", getLocationSchema());
        properties.put("type", nullableString()); // For Place/Food/Accom subtype
        properties.put("note", nullableString());

        // Make all fields required but nullable for strict mode compliance
        JSONArray required = new JSONArray();
        for (String key : properties.keySet()) {
            required.add(key);
        }
        schema.put("required", required);

        return schema;
    }

    private static JSONObject getLocationSchema() {
        JSONObject schema = new JSONObject();
        JSONArray typeArray = new JSONArray();
        typeArray.add("object");
        typeArray.add("null");
        schema.put("type", typeArray);
        schema.put("additionalProperties", false);
        
        JSONObject properties = new JSONObject();
        schema.put("properties", properties);
        
        properties.put("province", nullableString());
        properties.put("city", nullableString());
        properties.put("district", nullableString());
        properties.put("street", nullableString());
        properties.put("fullAddress", stringType()); // non-optional in swift
        properties.put("latitude", numberType()); // double in swift
        properties.put("longitude", numberType()); // double in swift
        
        JSONArray required = new JSONArray();
        for (String key : properties.keySet()) {
            required.add(key);
        }
        schema.put("required", required);
        
        return schema;
    }

    private static JSONObject getExternalLinkArraySchema() {
        JSONObject schema = new JSONObject();
        JSONArray typeArray = new JSONArray();
        typeArray.add("array");
        typeArray.add("null");
        schema.put("type", typeArray);
        
        JSONObject itemSchema = new JSONObject();
        itemSchema.put("type", "object");
        itemSchema.put("additionalProperties", false);
        JSONObject props = new JSONObject();
        props.put("label", stringType());
        props.put("url", stringType());
        itemSchema.put("properties", props);
        
        JSONArray req = new JSONArray();
        req.add("label");
        req.add("url");
        itemSchema.put("required", req);
        
        schema.put("items", itemSchema);
        return schema;
    }
    
    private static JSONObject getCostItemArraySchema() {
        JSONObject schema = new JSONObject();
        JSONArray typeArray = new JSONArray();
        typeArray.add("array");
        typeArray.add("null");
        schema.put("type", typeArray);
        
        JSONObject itemSchema = new JSONObject();
        itemSchema.put("type", "object");
        itemSchema.put("additionalProperties", false);
        JSONObject props = new JSONObject();
        props.put("name", stringType());
        props.put("amount", numberType());
        props.put("category", stringType());
        itemSchema.put("properties", props);
        
        JSONArray req = new JSONArray();
        req.add("name");
        req.add("amount");
        req.add("category");
        itemSchema.put("required", req);
        
        schema.put("items", itemSchema);
        return schema;
    }

    private static JSONObject stringType() {
        JSONObject type = new JSONObject();
        type.put("type", "string");
        return type;
    }
    
    private static JSONObject constString(String val) {
        JSONObject type = new JSONObject();
        type.put("type", "string");
        JSONArray enums = new JSONArray();
        enums.add(val);
        type.put("enum", enums);
        return type;
    }
    
    private static JSONObject integerType() {
        JSONObject type = new JSONObject();
        type.put("type", "integer");
        return type;
    }
    
    private static JSONObject numberType() {
        JSONObject type = new JSONObject();
        type.put("type", "number");
        return type;
    }

    private static JSONObject nullableString() {
        JSONObject type = new JSONObject();
        JSONArray types = new JSONArray();
        types.add("string");
        types.add("null");
        type.put("type", types);
        return type;
    }
    
    private static JSONObject nullableInteger() {
        JSONObject type = new JSONObject();
        JSONArray types = new JSONArray();
        types.add("integer");
        types.add("null");
        type.put("type", types);
        return type;
    }
    
    private static JSONObject nullableNumber() {
        JSONObject type = new JSONObject();
        JSONArray types = new JSONArray();
        types.add("number");
        types.add("null");
        type.put("type", types);
        return type;
    }
    
    private static JSONObject nullableArrayString() {
        JSONObject type = new JSONObject();
        JSONArray types = new JSONArray();
        types.add("array");
        types.add("null");
        type.put("type", types);
        type.put("items", stringType());
        return type;
    }
    
    private static JSONObject objectType(JSONObject props) {
        JSONObject type = new JSONObject();
        type.put("type", "object");
        type.put("additionalProperties", false);
        type.put("properties", props);
        JSONArray required = new JSONArray();
        for (String key : props.keySet()) {
            required.add(key);
        }
        type.put("required", required);
        return type;
    }
}
