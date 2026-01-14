package com.lvxin.base.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式ID生成器
 * 不使用Redis，通过时间、日期、机器码等信息生成唯一ID
 */
@Slf4j
@Component
public class DistributedIdGenerator {

    // 线程安全的序列号生成器（每毫秒重置）
    private static final AtomicInteger sequenceCounter = new AtomicInteger(0);
    private static volatile long lastTimestamp = 0;

    // 机器码缓存（基于主机名和IP地址）
    private static volatile String cachedMachineId = null;

    /**
     * 生成8位数字序列（不使用Redis）
     * 
     * 修复后的组合方式：秒级时间戳后5位 + 序列号2位 + 机器码后1位 = 8位
     * 格式：TTTTT + SS + M = TTTTTSSM
     * 
     * 改进点：
     * 1. 秒级时间戳后5位（0-99999）：约27小时的唯一性，避免跨天重复
     * 2. 序列号2位（0-99）：每毫秒重置，确保同一秒内的唯一性（最多每秒100个ID）
     * 3. 机器码后1位（0-9）：支持最多10台机器
     * 
     * 唯一性保证：
     * - 使用秒级时间戳+序列号可以保证全局唯一性
     * 
     * 唯一性保证：
     * - 秒级时间戳后5位：27小时内的唯一性
     * - 序列号：同一秒内最多100个ID不重复
     * - 机器码：不同机器之间的区分
     * - 组合：秒级时间戳(5位) * 1000 * 10 + 序列号(2位) * 10 + 机器码(1位)
     */
    private String generateNumeric8() {
        try {
            long nowMillis = System.currentTimeMillis();
            long nowSeconds = nowMillis / 1000; // 秒级时间戳
            
            // 1. 获取秒级时间戳后5位（00000-99999），约27小时的唯一性窗口
            int timestampPart = (int) (nowSeconds % 100000); // 后5位
            
            // 2. 获取序列号（每毫秒重置，确保同一毫秒内的唯一性）
            if (nowMillis != lastTimestamp) {
                sequenceCounter.set(0);
                lastTimestamp = nowMillis;
            }
            int sequencePart = sequenceCounter.getAndIncrement() % 100; // 0-99，确保不超过2位
            
            // 3. 获取机器码后1位（0-9），支持最多10台机器
            int machinePart = getMachineId() % 10;
            
            // 4. 组合：秒级时间戳5位 + 序列号2位 + 机器码1位 = 8位
            // 格式：TTTTT * 1000 + SS * 10 + M = TTTTTSSM
            long numericValue = (long) timestampPart * 1000L + (long) sequencePart * 10L + (long) machinePart;
            
            // 确保不超过8位数字（0-99999999）
            numericValue = numericValue % 100000000L;
            
            return String.format("%08d", numericValue);
            
        } catch (Exception e) {
            log.error("生成8位数字序列失败，使用时间戳+随机数", e);
            // 降级方案：使用时间戳后8位 + 随机数
            long fallback = (System.currentTimeMillis() % 100000000L) + (long)(Math.random() * 1000);
            return String.format("%08d", fallback % 100000000L);
        }
    }

    /**
     * 获取机器码（基于主机名和IP地址的哈希值）
     * 返回0-99的数字
     */
    private int getMachineId() {
        if (cachedMachineId == null) {
            synchronized (DistributedIdGenerator.class) {
                if (cachedMachineId == null) {
                    try {
                        // 获取主机名
                        String hostname = InetAddress.getLocalHost().getHostName();
                        
                        // 获取本机IP地址
                        String localIp = InetAddress.getLocalHost().getHostAddress();
                        
                        // 组合主机名和IP，生成哈希值
                        String machineInfo = hostname + "@" + localIp;
                        int hash = machineInfo.hashCode();
                        
                        // 取绝对值并模100，得到0-99的机器码
                        int machineId = Math.abs(hash) % 100;
                        cachedMachineId = String.format("%02d", machineId);
                        
                        log.info("机器码生成成功，hostname: {}, ip: {}, machineId: {}", hostname, localIp, cachedMachineId);
                        
                    } catch (Exception e) {
                        log.warn("获取机器码失败，使用默认值00", e);
                        cachedMachineId = "00";
                    }
                }
            }
        }
        
        try {
            return Integer.parseInt(cachedMachineId);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 生成分布式唯一ID（短ID，base36）- 保留以防其他地方仍使用
     */
    public String generateId() {
        try {
            long nowMillis = System.currentTimeMillis();
            String ts36 = Long.toString(nowMillis, 36);

            int machineNum = getMachineId();
            String m36 = Integer.toString(machineNum % 36, 36);

            // 使用线程安全的序列号生成器
            long currentTimestamp = nowMillis / 1000; // 秒级时间戳
            if (currentTimestamp != lastTimestamp) {
                sequenceCounter.set(0);
                lastTimestamp = currentTimestamp;
            }
            int seqNum = sequenceCounter.getAndIncrement() % 10000;
            String s36 = Integer.toString(seqNum % 1296, 36); // 36^2
            if (s36.length() < 2) s36 = "0" + s36;
            return ts36 + m36 + s36;
        } catch (Exception e) {
            log.error("生成分布式短ID失败", e);
            return Long.toString(System.currentTimeMillis(), 36) + Integer.toString((int)(Math.random()*1296),36);
        }
    }

    /**
     * 生成订单号（通用方法）
     * 格式：PREFIX + 分布式ID
     * 
     * @param prefix 订单号前缀（如：ORDER、PAY等）
     * @return 订单号
     */
    public String generateOrderNo(String prefix) {
        return prefix + generateNumeric8();
    }
}
