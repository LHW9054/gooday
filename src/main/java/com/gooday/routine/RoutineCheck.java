package com.gooday.routine;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "routine_checks")
public class RoutineCheck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long routineId;
    private LocalDate checkDate;

    protected RoutineCheck() {}

    public RoutineCheck(Long routineId, LocalDate checkDate) {
        this.routineId = routineId;
        this.checkDate = checkDate;
    }

    public Long getRoutineId() { return routineId; }
    public LocalDate getCheckDate() { return checkDate; }
}
