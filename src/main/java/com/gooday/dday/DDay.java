package com.gooday.dday;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ddays")
public class DDay {
    public enum Category { EXAM, BJJ, PAYDAY, CARD, ETC }
    public enum RepeatType { NONE, MONTHLY, YEARLY }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String title;
    @Enumerated(EnumType.STRING) private Category category = Category.ETC;
    private LocalDate targetDate;
    @Enumerated(EnumType.STRING) private RepeatType repeatType = RepeatType.NONE;
    private boolean pinned;
    private String memo;

    protected DDay() {}

    public DDay(Long userId) { this.userId = userId; }

    /** 반복 D-day 는 오늘 이후 가장 가까운 발생일. (31일 지정 → 짧은 달은 말일) */
    public LocalDate nextOccurrence(LocalDate today) {
        return switch (repeatType) {
            case NONE -> targetDate;
            case MONTHLY -> {
                int day = targetDate.getDayOfMonth();
                LocalDate c = today.withDayOfMonth(Math.min(day, today.lengthOfMonth()));
                if (c.isBefore(today)) {
                    LocalDate n = today.plusMonths(1);
                    c = n.withDayOfMonth(Math.min(day, n.lengthOfMonth()));
                }
                yield c;
            }
            case YEARLY -> {
                LocalDate c = targetDate.withYear(today.getYear());
                yield c.isBefore(today) ? targetDate.withYear(today.getYear() + 1) : c;
            }
        };
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public RepeatType getRepeatType() { return repeatType; }
    public void setRepeatType(RepeatType repeatType) { this.repeatType = repeatType; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
