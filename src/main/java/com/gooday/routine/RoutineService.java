package com.gooday.routine;

import com.gooday.common.ApiException;
import com.gooday.common.CurrentUser;
import com.gooday.common.Period;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoutineService {
    private final RoutineRepository routines;
    private final RoutineCheckRepository checks;

    public RoutineService(RoutineRepository routines, RoutineCheckRepository checks) {
        this.routines = routines;
        this.checks = checks;
    }

    /** 해당 날짜에 해야 할 루틴 + 체크 여부 */
    public List<RoutineDto.TodayItem> forDate(LocalDate date) {
        List<Routine> list = active().stream().filter(r -> r.isScheduledOn(date)).toList();
        Set<Long> done = checkedIds(list, date, date).getOrDefault(date, Set.of());
        return list.stream()
                .map(r -> new RoutineDto.TodayItem(r.getId(), r.getTitle(), r.getCategory(),
                        r.getDaysMask(), done.contains(r.getId())))
                .toList();
    }

    /** 체크 ↔ 해제 토글. 미래 날짜는 체크 불가. */
    @Transactional
    public boolean toggle(Long routineId, LocalDate date) {
        if (date.isAfter(LocalDate.now())) throw ApiException.badRequest("미래 날짜는 체크할 수 없어요.");
        find(routineId);
        Optional<RoutineCheck> existing = checks.findByRoutineIdAndCheckDate(routineId, date);
        if (existing.isPresent()) {
            checks.delete(existing.get());
            return false;
        }
        checks.save(new RoutineCheck(routineId, date));
        return true;
    }

    /** today 가 속한 기간의 루틴 달성률 (기간 시작 ~ 오늘) */
    public RoutineDto.Rate rate(Period period, LocalDate today) {
        LocalDate from = period.start(today);
        List<Routine> list = active();
        Map<LocalDate, Set<Long>> done = checkedIds(list, from, today);
        int total = 0, checked = 0;
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            Set<Long> ids = done.getOrDefault(d, Set.of());
            for (Routine r : list) {
                if (r.isScheduledOn(d)) {
                    total++;
                    if (ids.contains(r.getId())) checked++;
                }
            }
        }
        return RoutineDto.Rate.of(checked, total);
    }

    @Transactional
    public Long create(RoutineDto.Request req) {
        Routine r = new Routine(CurrentUser.ID, LocalDate.now());
        apply(r, req);
        return routines.save(r).getId();
    }

    @Transactional
    public void update(Long id, RoutineDto.Request req) { apply(find(id), req); }

    /** 삭제 대신 비활성화 → 과거 달성 기록은 보존 */
    @Transactional
    public void deactivate(Long id) { find(id).setActive(false); }

    private void apply(Routine r, RoutineDto.Request req) {
        r.setTitle(req.title().trim());
        if (req.category() != null) r.setCategory(req.category());
        if (req.daysMask() != null) r.setDaysMask(req.daysMask());
        if (req.sortOrder() != null) r.setSortOrder(req.sortOrder());
    }

    private List<Routine> active() {
        return routines.findByUserIdAndActiveTrueOrderBySortOrderAscIdAsc(CurrentUser.ID);
    }

    private Routine find(Long id) {
        return routines.findById(id).filter(r -> r.getUserId() == CurrentUser.ID)
                .orElseThrow(() -> ApiException.notFound("루틴"));
    }

    private Map<LocalDate, Set<Long>> checkedIds(List<Routine> list, LocalDate from, LocalDate to) {
        if (list.isEmpty()) return Map.of();
        List<Long> ids = list.stream().map(Routine::getId).toList();
        return checks.findByRoutineIdInAndCheckDateBetween(ids, from, to).stream()
                .collect(Collectors.groupingBy(RoutineCheck::getCheckDate,
                        Collectors.mapping(RoutineCheck::getRoutineId, Collectors.toSet())));
    }
}
