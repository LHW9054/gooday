package com.gooday.goal;

import com.gooday.common.ApiException;
import com.gooday.common.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GoalService {
    private final GoalRepository goals;
    private final GoalProgressRepository progress;

    public GoalService(GoalRepository goals, GoalProgressRepository progress) {
        this.goals = goals;
        this.progress = progress;
    }

    public List<GoalDto.Status> statuses(LocalDate today) {
        return goals.findByUserIdAndActiveTrueOrderByIdAsc(CurrentUser.ID).stream()
                .map(g -> status(g, today)).toList();
    }

    @Transactional
    public Long create(GoalDto.Request req) {
        Goal g = new Goal(CurrentUser.ID);
        apply(g, req);
        return goals.save(g).getId();
    }

    @Transactional
    public void update(Long id, GoalDto.Request req) { apply(find(id), req); }

    @Transactional
    public void deactivate(Long id) { find(id).setActive(false); }

    /** 진행 기록 추가 (예: 주짓수 1회, 공부 1.5시간). 음수로 정정 가능. */
    @Transactional
    public GoalDto.Status addProgress(Long id, GoalDto.ProgressRequest req) {
        Goal g = find(id);
        LocalDate date = req.date() == null ? LocalDate.now() : req.date();
        progress.save(new GoalProgress(g.getId(), date, req.value(), req.memo()));
        return status(g, LocalDate.now());
    }

    private GoalDto.Status status(Goal g, LocalDate today) {
        LocalDate from = g.getPeriodType().start(today);
        LocalDate to = g.getPeriodType().end(today);
        BigDecimal current = progress.sumBetween(g.getId(), from, to);
        int percent = current.multiply(BigDecimal.valueOf(100))
                .divide(g.getTargetValue(), 0, RoundingMode.HALF_UP).intValue();
        return new GoalDto.Status(g.getId(), g.getTitle(), g.getCategory(), g.getPeriodType(),
                g.getTargetValue().stripTrailingZeros(), current.stripTrailingZeros(), g.getUnit(),
                Math.max(0, percent), from, to);
    }

    private void apply(Goal g, GoalDto.Request r) {
        g.setTitle(r.title().trim());
        if (r.category() != null) g.setCategory(r.category());
        g.setPeriodType(r.periodType());
        g.setTargetValue(r.targetValue());
        if (r.unit() != null && !r.unit().isBlank()) g.setUnit(r.unit());
    }

    private Goal find(Long id) {
        return goals.findById(id).filter(g -> g.getUserId() == CurrentUser.ID)
                .orElseThrow(() -> ApiException.notFound("목표"));
    }
}
