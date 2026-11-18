package com.gooday.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** 일/주/월/년 기간. 주는 월요일 시작. */
public enum Period {
    DAILY, WEEKLY, MONTHLY, YEARLY;

    public LocalDate start(LocalDate d) {
        return switch (this) {
            case DAILY -> d;
            case WEEKLY -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> d.withDayOfMonth(1);
            case YEARLY -> d.withDayOfYear(1);
        };
    }

    public LocalDate end(LocalDate d) {
        return switch (this) {
            case DAILY -> d;
            case WEEKLY -> d.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case MONTHLY -> d.with(TemporalAdjusters.lastDayOfMonth());
            case YEARLY -> d.with(TemporalAdjusters.lastDayOfYear());
        };
    }
}
