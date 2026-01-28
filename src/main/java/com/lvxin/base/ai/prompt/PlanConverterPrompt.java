package com.lvxin.base.ai.prompt;

import org.springframework.stereotype.Component;

/**
 * 计划转换阶段的Prompt模板
 * 将Markdown格式的计划转换为JSON格式的计划表
 *
 * @author lvxin
 */
@Component
public class PlanConverterPrompt implements PromptTemplate {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是一位资深的**旅行规划专家**兼**数据架构师**。你的任务不仅仅是将Markdown计划转换为JSON，更重要的是对原始计划进行**深度优化、信息补全和专业润色**，最终输出为符合严格Schema的JSON数据。
            
            **输入计划（Markdown）：**
            %s
            
            **你的核心任务：**
            1. **深度分析与优化**：
               - 分析原始行程的合理性，如果发现时间冲突或不合理的路线，请在生成时进行微调优化。
               - 确保每天的行程安排松弛有度，符合游客的体力情况。
            
            2. **信息补全与丰富（非常重要）**：
               - **补全缺失细节**：如果原始计划只写了"去故宫"，你需要补全：
                 * highlights: ["中轴线建筑群", "珍宝馆", "延禧宫"]
                 * ticketInfo: "需提前7天在小程序预约，周一闭馆"
                 * stayDuration: "3-4小时"
                 * bestTime: "08:30入园避开人流"
                 * summary: "世界现存规模最大的古代宫殿建筑群，必打卡红墙黄瓦。"
               - **丰富餐饮信息**：如果只写了"吃午饭"，请根据地点推荐特色餐厅（如"四季民福烤鸭"），并预估人均消费。
               - **补全位置信息**：根据景点名称，自动推断并填充 location 字段（省市区、详细地址），尽可能精确。
            
            3. **数据结构化转换**：
               - 将优化后的内容严格转换为以下 JSON 结构。
            
            **JSON结构模板：**
            {
              "id": "itinerary-uuid（由系统生成，这里不需要填写）",
              "version": 1,
              "componentVersion": "v1",
              "title": "计划标题（如：深度漫游｜北京5天4夜历史文化之旅）",
              "coverImageURL": null,
              "days": [
                {
                  "type": "DayBlock",
                  "id": "day-1（由系统生成，这里不需要填写）",
                  "data": {
                    "dayIndex": 1,
                    "date": "YYYY-MM-DD",
                    "title": "Day1｜到达与初识（示例）",
                    "summary": "简短的一句亮点总结"
                  },
                  "blocks": [
                    {
                      "type": "SectionBlock",
                      "id": "section-1（由系统生成，这里不需要填写）",
                      "data": {
                        "title": "上午/下午/晚上",
                        "timeRange": "HH:mm-HH:mm",
                        "summary": "该时段活动概览"
                      },
                      "blocks": [
                        {
                          "type": "PlaceBlock",
                          "id": "place-1（由系统生成，这里不需要填写）",
                          "data": {
                            "name": "景点名称",
                            "type": "sightseeing/landmark/mall/park/museum",
                            "location": {
                              "province": "省",
                              "city": "市",
                              "district": "区",
                              "street": "街道",
                              "fullAddress": "完整地址",
                              "latitude": 0.0,
                              "longitude": 0.0
                            },
                            "stayDuration": "游玩时长（如：3小时）",
                            "ticketInfo": "门票与预约信息",
                            "highlights": ["亮点1", "亮点2"],
                            "bestTime": "最佳游玩时间",
                            "summary": "吸引人的简短描述"
                          }
                        },
                        {
                          "type": "FoodBlock",
                          "id": "food-1",
                          "data": {
                            "name": "餐厅名称",
                            "type": "restaurant/snack/cafe/bar",
                            "location": { ... },
                            "perCapita": 人均消费（数字）,
                            "mustTry": ["推荐菜1", "推荐菜2"],
                            "rating": 4.8,
                            "summary": "餐厅特色描述"
                          }
                        },
                        {
                          "type": "AccommodationBlock",
                          "id": "hotel-1",
                          "data": {
                            "name": "酒店名称",
                            "type": "hotel/hostel/homestay",
                            "location": { ... },
                            "checkIn": "14:00",
                            "checkOut": "12:00",
                            "pricePerNight": 预估价格（数字）,
                            "summary": "推荐理由"
                          }
                        },
                        {
                          "type": "TransportBlock",
                          "id": "transport-1",
                          "data": {
                            "transportType": "flight/train/taxi/car/bus/metro/walk",
                            "fromName": "起点",
                            "toName": "终点",
                            "departureTime": "HH:mm",
                            "arrivalTime": "HH:mm",
                            "cost": 预估费用（数字）,
                            "summary": "交通建议"
                          }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            
            **严格转换规则：**
            1. **完整性**：必须生成完整行程，**严禁截断**。如果Markdown有5天，JSON必须有5个DayBlock。
            2. **字段类型**：
               - `transportType` 只能是：flight, train, taxi, car, bus, metro, ship, walk
               - `type` (Place) 只能是：sightseeing, landmark, mall, park, museum
               - `cost`, `perCapita`, `pricePerNight` 必须是数字 (Double)。
            3. **Location**：必须尽力补全 location 对象。如果无法获取经纬度，设为 0.0。
            4. **Language**：所有描述性文字使用中文，风格专业、生动。
            
            请开始你的规划与转换工作：
            """;

    @Override
    public String generateSystemPrompt(Object context) {
        String markdownPlan = (String) context;
        if (markdownPlan == null || markdownPlan.isEmpty()) {
            throw new IllegalArgumentException("计划内容不能为空");
        }
        return String.format(SYSTEM_PROMPT_TEMPLATE, markdownPlan);
    }
}


