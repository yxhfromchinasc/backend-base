package com.lvxin.base.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期工具类
 */
public class DateUtil {
    private DateUtil() {
    }

    public static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    public static String format(LocalDateTime time) {
        return format(time, DEFAULT_PATTERN);
    }

    public static String format(LocalDateTime time, String pattern) {
        if (time == null) {
            return null;
        }
        return time.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime parseDateTime(String text) {
        return parseDateTime(text, DEFAULT_PATTERN);
    }

    public static LocalDateTime parseDateTime(String text, String pattern) {
        if (text == null) {
            return null;
        }
        return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
    }

    public static String formatDate(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDate parseDate(String text, String pattern) {
        if (text == null) {
            return null;
        }
        return LocalDate.parse(text, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime startOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date, LocalTime.MIN);
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.of(date, LocalTime.MAX);
    }

    public static long betweenDays(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    public static long betweenHours(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    public static long betweenMinutes(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    public static LocalDateTime toLocalDateTime(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), DEFAULT_ZONE);
    }

    public static long toEpochMilli(LocalDateTime time) {
        if (time == null) {
            return 0L;
        }
        return time.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(DEFAULT_ZONE);
    }

    public static LocalDateTime convertZone(LocalDateTime time, ZoneId from, ZoneId to) {
        if (time == null) {
            return null;
        }
        ZonedDateTime zdt = time.atZone(from);
        return zdt.withZoneSameInstant(to).toLocalDateTime();
    }
}
