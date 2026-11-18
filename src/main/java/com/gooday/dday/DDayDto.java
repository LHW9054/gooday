package com.gooday.dday;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public final class DDayDto {
    private DDayDto() {}

    public record Request(
            @NotBlank String title,
            DDay.Category category,
            @NotNull LocalDate targetDate,
            DDay.RepeatType repeatType,
            Boolean pinned,
            String memo) {}

    /** daysLeft: 0 = D-DAY, 양수 = D-n, 음수 = D+n(지난 날) */
    public record Response(Long id, String title, DDay.Category category, LocalDate date,
                           DDay.RepeatType repeatType, long daysLeft, String label,
                           Boolean pinned, String memo) {}
}
