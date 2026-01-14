package com.lvxin.base.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // -------------------- Common --------------------

    /**
     * 判断 key 是否存在
     *
     * @param key 键
     * @return true 表示存在
     */
    public Boolean exists(String key) {
        Assert.hasText(key, "key 不能为空");
        return redisTemplate.hasKey(key);
    }

    /**
     * 删除单个 key
     *
     * @param key 键
     * @return true 删除成功；false 不存在或删除失败
     */
    public Boolean delete(String key) {
        Assert.hasText(key, "key 不能为空");
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除多个 key
     *
     * @param keys 键集合
     * @return 实际删除数量
     */
    public Long delete(Collection<String> keys) {
        Assert.notEmpty(keys, "keys 不能为空");
        return redisTemplate.delete(keys);
    }

    /**
     * 设置过期时间
     *
     * @param key      键
     * @param duration 过期时长
     * @return 设置是否成功
     */
    public Boolean expire(String key, Duration duration) {
        Assert.hasText(key, "key 不能为空");
        Assert.notNull(duration, "duration 不能为空");
        return redisTemplate.expire(key, duration);
    }

    /**
     * 设置过期时间
     *
     * @param key     键
     * @param timeout 时长
     * @param unit    时间单位
     * @return 设置是否成功
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        Assert.hasText(key, "key 不能为空");
        Assert.notNull(unit, "unit 不能为空");
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 查询剩余过期时间
     *
     * @param key  键
     * @param unit 时间单位
     * @return 剩余时间；-2 表示 key 不存在；-1 表示永久不过期
     */
    public Long ttl(String key, TimeUnit unit) {
        Assert.hasText(key, "key 不能为空");
        Assert.notNull(unit, "unit 不能为空");
        Long ttl = redisTemplate.getExpire(key, unit);
        return ttl == null ? -2L : ttl; // -2 表示 key 不存在
    }

    // -------------------- String --------------------

    /**
     * 写入字符串/对象值
     *
     * @param key   键
     * @param value 值（将使用配置的 Value 序列化器）
     * @return 是否成功
     */
    public boolean set(String key, Object value) {
        Assert.hasText(key, "key 不能为空");
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value);
        return true;
    }

    /**
     * 写入值并设置过期时间
     *
     * @param key    键
     * @param value  值
     * @param expire 过期时长
     * @return 是否成功
     */
    public boolean set(String key, Object value, Duration expire) {
        Assert.hasText(key, "key 不能为空");
        Assert.notNull(expire, "expire 不能为空");
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, value, expire);
        return true;
    }

    /**
     * 仅当 key 不存在时写入，并设置过期时间
     *
     * @param key    键
     * @param value  值
     * @param expire 过期时长
     * @return true 设置成功；false 表示已存在未写入
     */
    public boolean setIfAbsent(String key, Object value, Duration expire) {
        Assert.hasText(key, "key 不能为空");
        Assert.notNull(expire, "expire 不能为空");
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        Boolean ok = ops.setIfAbsent(key, value, expire);
        return ok != null && ok;
    }

    /**
     * 获取原始值（不做类型转换）
     *
     * @param key 键
     * @return 存储的对象或 null
     */
    public Object getRaw(String key) {
        Assert.hasText(key, "key 不能为空");
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return ops.get(key);
    }

    /**
     * 按指定类型读取值
     *
     * @param key        键
     * @param targetType 目标类型
     * @return 转换后的值或 null
     */
    public <T> T get(String key, Class<T> targetType) {
        Assert.hasText(key, "key 不能为空");
        Assert.notNull(targetType, "targetType 不能为空");
        Object value = getRaw(key);
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }
        return objectMapper.convertValue(value, targetType);
    }

    /**
     * 数值递增（支持负数）
     *
     * @param key   键
     * @param delta 增量
     * @return 递增后的值
     */
    public Long incrementBy(String key, long delta) {
        Assert.hasText(key, "key 不能为空");
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        Long result = ops.increment(key, delta);
        return result == null ? 0L : result;
    }

    /**
     * 数值递减（本质为递增负数）
     *
     * @param key   键
     * @param delta 递减量
     * @return 递减后的值
     */
    public Long decrementBy(String key, long delta) {
        return incrementBy(key, -delta);
    }

    // -------------------- Hash --------------------

    /**
     * Hash 设值
     *
     * @param key   键
     * @param field 字段
     * @param value 值
     * @return 是否成功
     */
    public boolean hSet(String key, String field, Object value) {
        Assert.hasText(key, "key 不能为空");
        Assert.hasText(field, "field 不能为空");
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        ops.put(key, field, value);
        return true;
    }

    /**
     * Hash 批量设值
     *
     * @param key 键
     * @param map 字段-值集合
     * @return 是否成功
     */
    public boolean hSetAll(String key, Map<String, Object> map) {
        Assert.hasText(key, "key 不能为空");
        Assert.notEmpty(map, "map 不能为空");
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        ops.putAll(key, map);
        return true;
    }

    /**
     * Hash 获取原始值
     *
     * @param key   键
     * @param field 字段
     * @return 原始对象或 null
     */
    public Object hGetRaw(String key, String field) {
        Assert.hasText(key, "key 不能为空");
        Assert.hasText(field, "field 不能为空");
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        return ops.get(key, field);
    }

    /**
     * Hash 按类型获取
     *
     * @param key        键
     * @param field      字段
     * @param targetType 目标类型
     * @return 转换后的值或 null
     */
    public <T> T hGet(String key, String field, Class<T> targetType) {
        Object value = hGetRaw(key, field);
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }
        return objectMapper.convertValue(value, targetType);
    }

    /**
     * Hash 获取所有字段与值
     *
     * @param key 键
     * @return 字段-值映射
     */
    public Map<Object, Object> hGetAll(String key) {
        Assert.hasText(key, "key 不能为空");
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        return ops.entries(key);
    }

    /**
     * Hash 删除字段
     *
     * @param key    键
     * @param fields 要删除的字段
     * @return 实际删除数量
     */
    public Long hDel(String key, String... fields) {
        Assert.hasText(key, "key 不能为空");
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        return ops.delete(key, (Object[]) fields);
    }

    /**
     * 判断 Hash 字段是否存在
     *
     * @param key   键
     * @param field 字段
     * @return true 表示存在
     */
    public Boolean hHasKey(String key, String field) {
        Assert.hasText(key, "key 不能为空");
        Assert.hasText(field, "field 不能为空");
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        Boolean exist = ops.hasKey(key, field);
        return exist != null && exist;
    }

    // -------------------- List --------------------

    /**
     * 列表左侧推入
     *
     * @param key   键
     * @param value 值
     * @return 推入后长度
     */
    public Long lLeftPush(String key, Object value) {
        Assert.hasText(key, "key 不能为空");
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        Long len = ops.leftPush(key, value);
        return len == null ? 0L : len;
    }

    /**
     * 列表右侧推入
     *
     * @param key   键
     * @param value 值
     * @return 推入后长度
     */
    public Long lRightPush(String key, Object value) {
        Assert.hasText(key, "key 不能为空");
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        Long len = ops.rightPush(key, value);
        return len == null ? 0L : len;
    }

    /**
     * 列表左侧弹出
     *
     * @param key 键
     * @return 弹出的元素或 null
     */
    public Object lLeftPop(String key) {
        Assert.hasText(key, "key 不能为空");
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        return ops.leftPop(key);
    }

    /**
     * 列表右侧弹出
     *
     * @param key 键
     * @return 弹出的元素或 null
     */
    public Object lRightPop(String key) {
        Assert.hasText(key, "key 不能为空");
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        return ops.rightPop(key);
    }

    /**
     * 列表区间查询
     *
     * @param key   键
     * @param start 起始下标（包含）
     * @param end   结束下标（包含，-1 表示到末尾）
     * @return 列表切片
     */
    public List<Object> lRange(String key, long start, long end) {
        Assert.hasText(key, "key 不能为空");
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        return ops.range(key, start, end);
    }

    // -------------------- Set --------------------

    /**
     * Set 新增元素
     *
     * @param key    键
     * @param values 元素
     * @return 新增数量
     */
    public Long sAdd(String key, Object... values) {
        Assert.hasText(key, "key 不能为空");
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        Long n = ops.add(key, values);
        return n == null ? 0L : n;
    }

    /**
     * Set 新增元素并设置过期时间
     *
     * @param key    键
     * @param expire 过期时长
     * @param values 元素
     * @return 新增数量
     */
    public Long sAdd(String key, Duration expire, Object... values) {
        Assert.hasText(key, "key 不能为空");
        Assert.notNull(expire, "expire 不能为空");
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        Long n = ops.add(key, values);
        if (n != null && n > 0) {
            redisTemplate.expire(key, expire);
        }
        return n == null ? 0L : n;
    }

    /**
     * Set 删除元素
     *
     * @param key    键
     * @param values 元素
     * @return 删除数量
     */
    public Long sRem(String key, Object... values) {
        Assert.hasText(key, "key 不能为空");
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        Long n = ops.remove(key, values);
        return n == null ? 0L : n;
    }

    /**
     * 判断元素是否为 Set 成员
     *
     * @param key   键
     * @param value 元素
     * @return true 表示在集合中
     */
    public Boolean sIsMember(String key, Object value) {
        Assert.hasText(key, "key 不能为空");
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        Boolean m = ops.isMember(key, value);
        return m != null && m;
    }

    /**
     * 获取 Set 所有成员
     *
     * @param key 键
     * @return 成员集合
     */
    public Set<Object> sMembers(String key) {
        Assert.hasText(key, "key 不能为空");
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        return ops.members(key);
    }

    /**
     * 获取 Set 基数（大小）
     *
     * @param key 键
     * @return 基数
     */
    public Long sCard(String key) {
        Assert.hasText(key, "key 不能为空");
        SetOperations<String, Object> ops = redisTemplate.opsForSet();
        Long c = ops.size(key);
        return c == null ? 0L : c;
    }

    // -------------------- ZSet --------------------

    /**
     * ZSet 新增成员
     *
     * @param key   键
     * @param value 成员
     * @param score 分值
     * @return 是否新增成功
     */
    public Boolean zAdd(String key, Object value, double score) {
        Assert.hasText(key, "key 不能为空");
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();
        Boolean ok = ops.add(key, value, score);
        return ok != null && ok;
    }

    /**
     * ZSet 删除成员
     *
     * @param key    键
     * @param values 成员
     * @return 删除数量
     */
    public Long zRem(String key, Object... values) {
        Assert.hasText(key, "key 不能为空");
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();
        Long n = ops.remove(key, values);
        return n == null ? 0L : n;
    }

    /**
     * ZSet 区间升序查询
     *
     * @param key   键
     * @param start 起始排名（从 0 开始）
     * @param end   结束排名（-1 到末尾）
     * @return 成员集合
     */
    public Set<Object> zRange(String key, long start, long end) {
        Assert.hasText(key, "key 不能为空");
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();
        return ops.range(key, start, end);
    }

    /**
     * ZSet 区间降序查询
     *
     * @param key   键
     * @param start 起始排名
     * @param end   结束排名
     * @return 成员集合
     */
    public Set<Object> zRevRange(String key, long start, long end) {
        Assert.hasText(key, "key 不能为空");
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();
        return ops.reverseRange(key, start, end);
    }

    /**
     * 查询 ZSet 成员分值
     *
     * @param key   键
     * @param value 成员
     * @return 分值或 null
     */
    public Double zScore(String key, Object value) {
        Assert.hasText(key, "key 不能为空");
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();
        return ops.score(key, value);
    }
}


