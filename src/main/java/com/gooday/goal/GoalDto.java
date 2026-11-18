package com.gooday.goal;

import com.gooday.common.Period;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class GoalDto {
    private GoalDto() {}

    public record Request(@NotBlank String title, String category, @NotNull Period periodType,
                          @NotNull @Positive BigDecimal targetValue, String unit) {}

    public record ProgressRequest(@NotNull BigDecimal value, LocalDate date, String memo) {}

    /** 현재 기간(이번 주/이번 달/올해…)의 진행 상황 */
    public record Status(Long id, String title, String category, Period periodType,
                         BigDecimal target, BigDecimal current, String unit, int percent,
                         LocalDate periodStart, LocalDate periodEnd) {}
}
