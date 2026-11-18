package com.gooday.goal;

import com.gooday.common.Period;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "goals")
public class Goal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String title;
    private String category = "LIFE";
    @Enumerated(EnumType.STRING) private Period periodType;
    private BigDecimal targetValue;
    private String unit = "회";
    private boolean active = true;

    protected Goal() {}

    public Goal(Long userId) { this.userId = userId; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Period getPeriodType() { return periodType; }
    public void setPeriodType(Period periodType) { this.periodType = periodType; }
    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
