package com.gooday.goal;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalProgressRepository extends JpaRepository<GoalProgress, Long> {
    @Query("select coalesce(sum(p.value), 0) from GoalProgress p "
         + "where p.goalId = :goalId and p.logDate between :from and :to")
    BigDecimal sumBetween(@Param("goalId") Long goalId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
