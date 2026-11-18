package com.gooday.dashboard;

import com.gooday.dday.DDayDto;
import com.gooday.goal.GoalDto;
import com.gooday.routine.RoutineDto;
import java.time.LocalDate;
import java.util.List;

public record DashboardDto(
        LocalDate today,
        List<DDayDto.Response> ddays,
        List<RoutineDto.TodayItem> routines,
        Rates routineRates,
        List<GoalDto.Status> goals) {

    public record Rates(RoutineDto.Rate daily, RoutineDto.Rate weekly,
                        RoutineDto.Rate monthly, RoutineDto.Rate yearly) {}
}
