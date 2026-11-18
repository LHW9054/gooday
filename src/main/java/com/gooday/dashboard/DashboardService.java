package com.gooday.dashboard;

import com.gooday.common.Period;
import com.gooday.dday.DDayService;
import com.gooday.goal.GoalService;
import com.gooday.routine.RoutineService;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** 대시보드 한 화면에 필요한 데이터를 한 번에 모아 준다 (요청 1번으로 화면 완성). */
@Service
public class DashboardService {
    private final DDayService ddays;
    private final RoutineService routines;
    private final GoalService goals;

    public DashboardService(DDayService ddays, RoutineService routines, GoalService goals) {
        this.ddays = ddays;
        this.routines = routines;
        this.goals = goals;
    }

    public DashboardDto build(LocalDate today) {
        var rates = new DashboardDto.Rates(
                routines.rate(Period.DAILY, today),
                routines.rate(Period.WEEKLY, today),
                routines.rate(Period.MONTHLY, today),
                routines.rate(Period.YEARLY, today));
        return new DashboardDto(today, ddays.list(today), routines.forDate(today), rates, goals.statuses(today));
    }
}
