package com.gooday.routine;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class RoutineDto {
    private RoutineDto() {}

    public record Request(@NotBlank String title, String category,
                          @Min(1) @Max(127) Integer daysMask, Integer sortOrder) {}

    public record TodayItem(Long id, String title, String category, int daysMask, boolean checked) {}

    /** 기간 달성률: 체크 수 / 해야 했던 수 (오늘까지만 계산) */
    public record Rate(int done, int total, int percent) {
        public static Rate of(int done, int total) {
            return new Rate(done, total, total == 0 ? 0 : Math.round(done * 100f / total));
        }
    }
}
