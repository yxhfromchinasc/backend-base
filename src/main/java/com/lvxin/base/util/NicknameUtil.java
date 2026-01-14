package com.lvxin.base.util;

import com.lvxin.base.service.SystemConfigService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 昵称工具类
 * 用于生成随机昵称
 * 昵称列表从系统配置中读取，配置键为：random_nickname_list
 */
@Slf4j
@Component
public class NicknameUtil {

    /**
     * 配置键：随机昵称列表
     */
    private static final String CONFIG_KEY_RANDOM_NICKNAME_LIST = "random_nickname_list";

    /**
     * 默认昵称列表（当系统配置不存在时使用）
     */
    private static final List<String> DEFAULT_NICKNAME_LIST = List.of(
            "阳光的蜜蜂", "快乐的狮子", "温柔的天鹅", "山峰的凤凰", "月光的猫头鹰",
            "可爱的熊猫", "闪亮的孔雀", "城市的猴子", "海洋的企鹅", "未来的龙",
            "彩虹的袋鼠", "温暖的狐狸", "阳光的蜘蛛", "梦幻的兔子", "甜蜜的考拉",
            "勇敢的战士", "可爱的猴子", "星辰的猫头鹰", "快乐的企鹅", "艺术的画家",
            "月光的袋鼠", "热情的狐狸", "梦幻的独角兽", "温柔的长颈鹿", "智慧的科学家",
            "可爱的熊猫", "快乐的猫头鹰", "优雅的女王", "诗意的凤凰", "温暖的蝴蝶",
            "闪亮的星星", "甜蜜的蜜糖", "梦幻的云朵", "温柔的微风", "快乐的小鹿",
            "可爱的海豚", "阳光的向日葵", "月光的萤火虫", "彩虹的彩霞", "勇敢的骑士",
            "智慧的书卷", "热情的火焰", "甜蜜的糖果", "梦幻的城堡", "温柔的浪花",
            "可爱的松鼠", "闪亮的珍珠", "快乐的音符", "温暖的阳光", "诗意的河流",
            "优雅的芭蕾", "星辰的梦想", "海洋的贝壳", "山峰的雪花", "彩虹的桥",
            "甜蜜的微笑", "梦幻的精灵", "温柔的月光", "可爱的兔子", "勇敢的探险家",
            "智慧的猫头鹰", "热情的向导", "闪亮的钻石", "快乐的舞蹈", "温暖的拥抱",
            "诗意的画笔", "优雅的天使", "星辰的海洋", "阳光的草地", "月光的森林",
            "彩虹的花园", "甜蜜的果实", "梦幻的彩虹", "温柔的雨露", "可爱的熊宝宝",
            "勇敢的英雄", "智慧的学者", "热情的旅人", "闪亮的晨曦", "快乐的旋律",
            "温暖的炉火", "诗意的夕阳", "优雅的公主", "星辰的翅膀", "海洋的珊瑚",
            "山峰的云海", "彩虹的露珠", "甜蜜的蜂蜜", "梦幻的泡泡", "温柔的轻风",
            "可爱的狐狸", "勇敢的守护者", "智慧的灯塔", "热情的阳光", "闪亮的银河",
            "快乐的节日", "温暖的家", "诗意的星空", "优雅的舞者", "星辰的微笑",
            "阳光的花朵", "月光的湖泊", "彩虹的蝴蝶", "甜蜜的回忆", "梦幻的仙子",
            "温柔的晨光", "可爱的企鹅", "勇敢的先锋", "智慧的导师", "热情的火焰鸟",
            "闪亮的露水", "快乐的歌声", "温暖的怀抱", "诗意的山川", "优雅的王子",
            "星辰的旅途", "海洋的浪花", "山峰的松树", "彩虹的彩带", "甜蜜的巧克力",
            "梦幻的宫殿", "温柔的雪花", "可爱的猫咪", "勇敢的冒险家", "智慧的哲人",
            "热情的舞伴", "闪亮的彩虹", "快乐的派对", "温暖的阳光雨", "诗意的云彩",
            "优雅的孔雀", "星辰的守护", "阳光的绿叶", "月光的银辉", "彩虹的梦境",
            "甜蜜的果园", "梦幻的魔法", "温柔的春风", "可爱的熊", "勇敢的勇士",
            "智慧的精灵", "热情的夏日", "闪亮的星河", "快乐的欢笑", "温暖的冬阳",
            "诗意的秋叶", "优雅的白天鹅", "星辰的传说", "海洋的珍宝", "山峰的晨雾",
            "彩虹的微笑", "甜蜜的蛋糕", "梦幻的童话", "温柔的微光", "可爱的海豹",
            "勇敢的领航者", "智慧的向日葵", "热情的火花", "闪亮的晨星", "快乐的聚会",
            "温暖的港湾", "诗意的月影", "优雅的舞步", "星辰的旅人", "阳光的果树",
            "月光的花瓣", "彩虹的奇迹", "甜蜜的香气", "梦幻的奇境", "温柔的细雨",
            "可爱的松鼠", "勇敢的梦想家", "智慧的画家", "热情的歌手", "闪亮的宝石",
            "快乐的节日灯", "温暖的回忆", "诗意的风景", "优雅的艺术家", "星辰的旋律",
            "海洋的蓝天", "山峰的绿意", "彩虹的希望", "甜蜜的吻", "梦幻的星空",
            "温柔的拥抱", "可爱的鸟儿", "勇敢的飞行员", "智慧的书籍", "热情的阳光",
            "闪亮的浪花", "快乐的童年", "温暖的春天", "诗意的夏夜", "优雅的秋风",
            "星辰的祝福", "阳光的微笑", "月光的温柔", "彩虹的幸福", "甜蜜的永恒"
    );

    @Resource
    private SystemConfigService systemConfigService;

    private static SystemConfigService staticSystemConfigService;

    private static final Random RANDOM = new Random();

    @PostConstruct
    public void init() {
        staticSystemConfigService = systemConfigService;
    }

    /**
     * 从系统配置获取昵称列表
     * @return 昵称列表
     */
    private static List<String> getNicknameList() {
        try {
            String configValue = staticSystemConfigService.getConfigValue(CONFIG_KEY_RANDOM_NICKNAME_LIST);
            if (configValue != null && !configValue.trim().isEmpty()) {
                // 从JSON字符串解析昵称列表
                List<String> nicknameList = JsonUtil.toList(configValue, String.class);
                if (nicknameList != null && !nicknameList.isEmpty()) {
                    return nicknameList;
                }
            }
        } catch (Exception e) {
            log.warn("从系统配置读取昵称列表失败，使用默认列表: {}", e.getMessage());
        }
        // 如果配置不存在或解析失败，使用默认列表
        return DEFAULT_NICKNAME_LIST;
    }

    /**
     * 获取随机昵称
     * @return 随机昵称
     */
    public static String getRandomNickname() {
        List<String> nicknameList = getNicknameList();
        if (nicknameList == null || nicknameList.isEmpty()) {
            log.warn("昵称列表为空，返回默认昵称");
            return "用户";
        }
        int index = RANDOM.nextInt(nicknameList.size());
        return nicknameList.get(index);
    }

    /**
     * 判断是否需要使用随机昵称
     * 当昵称为空、null或"用户"时，返回true
     * @param nickname 原始昵称
     * @return 是否需要使用随机昵称
     */
    public static boolean shouldUseRandomNickname(String nickname) {
        return nickname == null || nickname.trim().isEmpty() || "用户".equals(nickname.trim());
    }
}

