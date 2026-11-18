package com.gooday.dday;

import com.gooday.common.ApiException;
import com.gooday.common.CurrentUser;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DDayService {
    private final DDayRepository repo;

    public DDayService(DDayRepository repo) { this.repo = repo; }

    /** 고정(pinned) 먼저, 그다음 가까운 순. 지난 일회성 D-day 는 뒤로. */
    public List<DDayDto.Response> list(LocalDate today) {
        return repo.findByUserId(CurrentUser.ID).stream()
                .map(d -> toResponse(d, today))
                .sorted(Comparator.comparing((DDayDto.Response r) -> r.daysLeft() < 0)
                        .thenComparing(r -> !r.pinned())
                        .thenComparingLong(r -> Math.abs(r.daysLeft())))
                .toList();
    }

    @Transactional
    public DDayDto.Response create(DDayDto.Request req) {
        DDay d = new DDay(CurrentUser.ID);
        apply(d, req);
        return toResponse(repo.save(d), LocalDate.now());
    }

    @Transactional
    public DDayDto.Response update(Long id, DDayDto.Request req) {
        DDay d = find(id);
        apply(d, req);
        return toResponse(d, LocalDate.now());
    }

    @Transactional
    public void delete(Long id) { repo.delete(find(id)); }

    private DDay find(Long id) {
        return repo.findById(id).filter(d -> d.getUserId() == CurrentUser.ID)
                .orElseThrow(() -> ApiException.notFound("D-day"));
    }

    private void apply(DDay d, DDayDto.Request r) {
        d.setTitle(r.title().trim());
        d.setCategory(r.category() == null ? DDay.Category.ETC : r.category());
        d.setTargetDate(r.targetDate());
        d.setRepeatType(r.repeatType() == null ? DDay.RepeatType.NONE : r.repeatType());
        d.setPinned(Boolean.TRUE.equals(r.pinned()));
        d.setMemo(r.memo());
    }

    static DDayDto.Response toResponse(DDay d, LocalDate today) {
        LocalDate date = d.nextOccurrence(today);
        long left = ChronoUnit.DAYS.between(today, date);
        String label = left == 0 ? "D-DAY" : left > 0 ? "D-" + left : "D+" + (-left);
        return new DDayDto.Response(d.getId(), d.getTitle(), d.getCategory(), date,
                d.getRepeatType(), left, label, d.isPinned(), d.getMemo());
    }
}
