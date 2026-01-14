package com.lvxin.base.util;

import java.math.BigDecimal;

/**
 * 距离计算工具类
 * 提供地理位置距离计算相关的方法
 * <p>
 * 功能说明：
 * - 使用Haversine公式计算地球表面两点间的球面距离
 * - 支持BigDecimal和Double两种参数类型
 * - 提供便捷的范围判断方法
 * - 距离结果保留2位小数精度
 * <p>
 * 使用场景：
 * - 驿站服务半径计算
 * - 地址与驿站距离判断
 * - 地理位置相关业务逻辑
 *
 * @author system
 * @since 1.0.0
 */
public class DistanceUtil {

    /**
     * 地球半径（公里）
     * 用于Haversine公式计算地球表面距离
     * 参考值：6371.0公里（地球平均半径）
     */
    private static final double EARTH_RADIUS = 6371.0;

    /**
     * 计算两点间距离（公里）
     * 使用Haversine公式计算地球表面两点间的球面距离
     * <p>
     * 算法说明：
     * 1. 将角度转换为弧度
     * 2. 计算纬度差和经度差
     * 3. 使用Haversine公式计算球面距离
     * 4. 返回保留2位小数的距离结果
     *
     * @param lat1 纬度1 - 第一个点的纬度坐标
     * @param lon1 经度1 - 第一个点的经度坐标
     * @param lat2 纬度2 - 第二个点的纬度坐标
     * @param lon2 经度2 - 第二个点的经度坐标
     * @return 距离（公里），保留2位小数。如果任一参数为null，返回Double.MAX_VALUE
     */
    public static BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return BigDecimal.valueOf(Double.MAX_VALUE);
        }

        // 将角度转换为弧度
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lon1Rad = Math.toRadians(lon1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double lon2Rad = Math.toRadians(lon2.doubleValue());

        // 计算纬度差和经度差
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        // Haversine公式
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;

        return BigDecimal.valueOf(distance).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 计算两点间距离（公里）- Double版本
     * 使用Haversine公式计算地球表面两点间的球面距离
     * <p>
     * 这是BigDecimal版本的重载方法，内部会转换为BigDecimal进行计算
     *
     * @param lat1 纬度1 - 第一个点的纬度坐标
     * @param lon1 经度1 - 第一个点的经度坐标
     * @param lat2 纬度2 - 第二个点的纬度坐标
     * @param lon2 经度2 - 第二个点的经度坐标
     * @return 距离（公里），保留2位小数。如果任一参数为null，返回Double.MAX_VALUE
     */
    public static BigDecimal calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return BigDecimal.valueOf(Double.MAX_VALUE);
        }

        return calculateDistance(
                BigDecimal.valueOf(lat1),
                BigDecimal.valueOf(lon1),
                BigDecimal.valueOf(lat2),
                BigDecimal.valueOf(lon2)
        );
    }

    /**
     * 检查两点间距离是否在指定范围内
     * <p>
     * 业务场景：
     * - 判断驿站是否在地址的服务半径内
     * - 判断地址是否在驿站的服务范围内
     * - 地理位置相关的业务范围判断
     *
     * @param lat1   纬度1 - 第一个点的纬度坐标
     * @param lon1   经度1 - 第一个点的经度坐标
     * @param lat2   纬度2 - 第二个点的纬度坐标
     * @param lon2   经度2 - 第二个点的经度坐标
     * @param radius 半径（公里） - 服务半径范围
     * @return true 在范围内，false 超出范围。如果radius为null或<=0，返回false
     */
    public static boolean isWithinRadius(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2, BigDecimal radius) {
        if (radius == null || radius.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance.compareTo(radius) <= 0;
    }

    /**
     * 检查两点间距离是否在指定范围内 - Double版本
     * <p>
     * 这是BigDecimal版本的重载方法，内部会转换为BigDecimal进行计算
     *
     * @param lat1   纬度1 - 第一个点的纬度坐标
     * @param lon1   经度1 - 第一个点的经度坐标
     * @param lat2   纬度2 - 第二个点的纬度坐标
     * @param lon2   经度2 - 第二个点的经度坐标
     * @param radius 半径（公里） - 服务半径范围
     * @return true 在范围内，false 超出范围。如果radius为null或<=0，返回false
     */
    public static boolean isWithinRadius(Double lat1, Double lon1, Double lat2, Double lon2, Double radius) {
        if (radius == null || radius <= 0) {
            return false;
        }

        BigDecimal distance = calculateDistance(lat1, lon1, lat2, lon2);
        return distance.compareTo(BigDecimal.valueOf(radius)) <= 0;
    }

}
