package com.gooday.goal;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "goal_progress")
public class GoalProgress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long goalId;
    private LocalDate logDate;
    private BigDecimal value;
    private String memo;

    protected GoalProgress() {}

    public GoalProgress(Long goalId, LocalDate logDate, BigDecimal value, String memo) {
        this.goalId = goalId;
        this.logDate = logDate;
        this.value = value;
        this.memo = memo;
    }

    public Long getId() { return id; }
    public Long getGoalId() { return goalId; }
    public LocalDate getLogDate() { return logDate; }
    public BigDecimal getValue() { return value; }
    public String getMemo() { return memo; }
}
