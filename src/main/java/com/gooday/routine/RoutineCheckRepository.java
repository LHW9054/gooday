package com.gooday.routine;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineCheckRepository extends JpaRepository<RoutineCheck, Long> {
    Optional<RoutineCheck> findByRoutineIdAndCheckDate(Long routineId, LocalDate date);
    List<RoutineCheck> findByRoutineIdInAndCheckDateBetween(Collection<Long> routineIds, LocalDate from, LocalDate to);
}
