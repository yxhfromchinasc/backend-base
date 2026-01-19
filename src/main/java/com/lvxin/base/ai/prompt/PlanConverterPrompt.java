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
            你是一个计划表转换器。将Markdown格式的旅行计划转换为标准的JSON格式计划表。
            
            **输入计划（Markdown）：**
            %s
            
            **输出要求：**
            1. 转换为符合以下JSON结构的计划表（严格按照字段要求，使用type-id-data结构）：
            {
              "id": "itinerary-uuid（由系统生成，这里不需要填写）",
              "version": 1,
              "componentVersion": "v1",
              "title": "计划标题",
              "coverImageURL": null,
              "days": [
                {
                  "type": "DayBlock",
                  "id": "day-1（由系统生成，这里不需要填写）",
                  "data": {
                    "dayIndex": 1,
                    "date": "YYYY-MM-DD",
                    "title": "Day1｜标题",
                    "summary": "当天概述（可选）"
                  },
                  "blocks": [
                    {
                      "type": "SectionBlock",
                      "id": "section-1（由系统生成，这里不需要填写）",
                      "data": {
                        "title": "上午/下午/晚上",
                        "timeRange": "HH:mm-HH:mm",
                        "summary": "时间段概述（可选）"
                      },
                      "blocks": [
                        {
                          "type": "TransportBlock",
                          "id": "transport-1（由系统生成，这里不需要填写）",
                          "data": {
                            "transportType": "flight/train/taxi/car/bus/metro/ship/walk",
                            "fromName": "起点名称",
                            "fromLocation": {
                              "province": "省",
                              "city": "市",
                              "district": "区",
                              "street": "街道",
                              "fullAddress": "完整地址（省市区街道）",
                              "latitude": 0.0,
                              "longitude": 0.0
                            },
                            "toName": "终点名称",
                            "toLocation": {
                              "province": "省",
                              "city": "市",
                              "district": "区",
                              "street": "街道",
                              "fullAddress": "完整地址（省市区街道）",
                              "latitude": 0.0,
                              "longitude": 0.0
                            },
                            "departureTime": "HH:mm",
                            "arrivalTime": "HH:mm",
                            "cost": 费用（数字，Double类型）
                          }
                        },
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
                              "fullAddress": "完整地址（省市区街道）",
                              "latitude": 0.0,
                              "longitude": 0.0
                            },
                            "stayDuration": "预计游玩时间（如：2-3小时）",
                            "ticketInfo": "门票信息（如有）",
                            "highlights": ["亮点1", "亮点2"],
                            "summary": "简要描述"
                          }
                        },
                        {
                          "type": "FoodBlock",
                          "id": "food-1（由系统生成，这里不需要填写）",
                          "data": {
                            "name": "餐厅名称",
                            "type": "restaurant/snack/cafe/bar",
                            "location": {
                              "province": "省",
                              "city": "市",
                              "district": "区",
                              "street": "街道",
                              "fullAddress": "完整地址（省市区街道）",
                              "latitude": 0.0,
                              "longitude": 0.0
                            },
                            "perCapita": 人均消费（数字，Double类型）,
                            "mustTry": ["必点菜品1", "必点菜品2"],
                            "rating": 评分（数字，0-5，如有）
                          }
                        },
                        {
                          "type": "AccommodationBlock",
                          "id": "accommodation-1（由系统生成，这里不需要填写）",
                          "data": {
                            "name": "酒店名称",
                            "type": "hotel/hostel/homestay",
                            "location": {
                              "province": "省",
                              "city": "市",
                              "district": "区",
                              "street": "街道",
                              "fullAddress": "完整地址（省市区街道）",
                              "latitude": 0.0,
                              "longitude": 0.0
                            },
                            "checkIn": "HH:mm",
                            "checkOut": "HH:mm",
                            "pricePerNight": 每晚价格（数字，Double类型）
                          }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            
            2. **转换规则（严格遵循）：**
            - **顶层字段**：id、version、componentVersion由系统生成，不需要填写；title从Markdown标题提取；coverImageURL设为null
            - **DayBlock结构**：必须包含type("DayBlock")、id（系统生成）、data（dayIndex从1开始递增、date、title、summary可选）、blocks数组
            - **SectionBlock结构**：必须包含type("SectionBlock")、id（系统生成）、data（title、timeRange、summary可选）、blocks数组
            - **时间线解析**：
              * 如果Markdown包含时间线格式（如"7:30"、"8:00"、"10:30-11:30"），必须按照时间顺序解析
              * 单个时间点（如"7:30"）应该创建一个SectionBlock，timeRange设为该时间点（如"7:30-7:30"或"7:30"）
              * 时间段（如"10:30-11:30"）应该创建一个SectionBlock，timeRange设为该时间段
              * 按时间顺序将活动分组到对应的SectionBlock中
            - **日期格式**：YYYY-MM-DD
            - **时间格式**：时间段使用"HH:mm-HH:mm"，具体时间使用"HH:mm"
            - **地址结构**：location必须包含province、city、district、street、fullAddress（完整地址）、latitude、longitude；如果Markdown中只有名称，尝试从名称推断地址；如果缺少经纬度，设置为0.0
            - **费用**：必须是数字类型（Double），单位统一为元（人民币）
            - **transportType**：必须使用标准值（flight/train/taxi/car/bus/metro/ship/walk）
              * 如果Markdown中写的是"地铁"，转换为"metro"
              * 如果写的是"公交"，转换为"bus"
              * 如果写的是"出租车"或"打车"，转换为"taxi"
              * 如果写的是"高铁"或"火车"，转换为"train"
              * 如果写的是"飞机"或"航班"，转换为"flight"
              * 如果写的是"步行"，转换为"walk"
              * 如果写的是"自驾"或"开车"，转换为"car"
            - **PlaceBlock的type**：必须使用标准值（sightseeing/landmark/mall/park/museum）
            - **FoodBlock的type**：必须使用标准值（restaurant/snack/cafe/bar）
            - **AccommodationBlock的type**：必须使用标准值（hotel/hostel/homestay）
            - **评分**：如果有，必须是0-5之间的数字（Double类型）
            - **数组字段**：highlights、mustTry等如果是空，使用空数组[]或省略
            - **可选字段**：如果Markdown中没有，可以省略或设置为null
            - **层级关系**：DayBlock的blocks可以包含SectionBlock或直接包含ItemBlock；SectionBlock的blocks只能包含ItemBlock
            - **活动识别**：
              * 如果包含"交通方式"、"起点"、"终点"等关键词，识别为TransportBlock
              * 如果包含"景点"、"游览"、"打卡"等关键词，识别为PlaceBlock
              * 如果包含"餐厅"、"午餐"、"晚餐"、"人均"等关键词，识别为FoodBlock
              * 如果包含"酒店"、"住宿"、"入住"等关键词，识别为AccommodationBlock
            
            3. **输出要求：**
            - 只输出JSON，不要其他内容
            - JSON要格式正确，可以被解析
            - 确保所有必需的字段都有值
            
            请开始转换：
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
