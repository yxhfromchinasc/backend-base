package com.lvxin.base.util;

import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额处理工具类
 * 提供金额的解析、验证和格式化功能
 */
@Slf4j
public class AmountUtil {

    /**
     * 金额精度（保留2位小数）
     */
    public static final int AMOUNT_SCALE = 2;

    /**
     * 金额舍入模式
     */
    public static final RoundingMode AMOUNT_ROUNDING_MODE = RoundingMode.HALF_UP;


    /**
     * 解析金额字符串
     *
     * @param amountStr 金额字符串
     * @return 解析后的金额
     * @throws BusinessException 解析失败时抛出异常
     */
    public static BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "金额不能为空");
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr.trim());

            // 验证金额范围
            validateAmountRange(amount);

            // 设置精度
            return amount.setScale(AMOUNT_SCALE, AMOUNT_ROUNDING_MODE);

        } catch (NumberFormatException e) {
            log.warn("金额格式不正确: {}", amountStr);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "金额格式不正确");
        }
    }

    /**
     * 验证金额范围
     *
     * @param amount 金额
     * @throws BusinessException 金额超出范围时抛出异常
     */
    public static void validateAmountRange(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "金额不能为空");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "金额必须大于0");
        }
    }

    /**
     * 格式化金额为字符串
     *
     * @param amount 金额
     * @return 格式化后的字符串
     */
    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }

        return amount.setScale(AMOUNT_SCALE, AMOUNT_ROUNDING_MODE).toString();
    }

    /**
     * 计算金额总和
     *
     * @param amounts 金额数组
     * @return 总和
     */
    public static BigDecimal sum(BigDecimal... amounts) {
        if (amounts == null || amounts.length == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal amount : amounts) {
            if (amount != null) {
                sum = sum.add(amount);
            }
        }

        return sum.setScale(AMOUNT_SCALE, AMOUNT_ROUNDING_MODE);
    }

    /**
     * 计算金额平均值
     *
     * @param amounts 金额数组
     * @return 平均值
     */
    public static BigDecimal average(BigDecimal... amounts) {
        if (amounts == null || amounts.length == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = sum(amounts);
        return sum.divide(new BigDecimal(amounts.length), AMOUNT_SCALE, AMOUNT_ROUNDING_MODE);
    }

    /**
     * 比较两个金额
     *
     * @param amount1 金额1
     * @param amount2 金额2
     * @return 比较结果：-1(小于), 0(等于), 1(大于)
     */
    public static int compare(BigDecimal amount1, BigDecimal amount2) {
        if (amount1 == null && amount2 == null) {
            return 0;
        }
        if (amount1 == null) {
            return -1;
        }
        if (amount2 == null) {
            return 1;
        }

        return amount1.compareTo(amount2);
    }

    /**
     * 检查金额是否相等
     *
     * @param amount1 金额1
     * @param amount2 金额2
     * @return 是否相等
     */
    public static boolean equals(BigDecimal amount1, BigDecimal amount2) {
        return compare(amount1, amount2) == 0;
    }

    /**
     * 检查金额是否为零
     *
     * @param amount 金额
     * @return 是否为零
     */
    public static boolean isZero(BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 检查金额是否为正数
     *
     * @param amount 金额
     * @return 是否为正数
     */
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 检查金额是否为负数
     *
     * @param amount 金额
     * @return 是否为负数
     */
    public static boolean isNegative(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }
}
