package com.gooday.dday;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DDayRepository extends JpaRepository<DDay, Long> {
    List<DDay> findByUserId(Long userId);
}
