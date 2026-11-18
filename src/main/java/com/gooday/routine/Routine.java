package com.gooday.routine;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Entity
@Table(name = "routines")
public class Routine {
    public static final int EVERY_DAY = 127;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String title;
    private String category = "LIFE";
    /** 월=1, 화=2, 수=4, 목=8, 금=16, 토=32, 일=64 */
    private int daysMask = EVERY_DAY;
    private int sortOrder;
    private boolean active = true;
    private LocalDate startDate;

    protected Routine() {}

    public Routine(Long userId, LocalDate startDate) {
        this.userId = userId;
        this.startDate = startDate;
    }

    public boolean isScheduledOn(LocalDate date) {
        if (!active || date.isBefore(startDate)) return false;
        return (daysMask & bit(date.getDayOfWeek())) != 0;
    }

    static int bit(DayOfWeek dow) { return 1 << (dow.getValue() - 1); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getDaysMask() { return daysMask; }
    public void setDaysMask(int daysMask) { this.daysMask = daysMask; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDate getStartDate() { return startDate; }
}
