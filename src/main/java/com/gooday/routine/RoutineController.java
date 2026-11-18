package com.gooday.routine;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routines")
public class RoutineController {
    private final RoutineService service;

    public RoutineController(RoutineService service) { this.service = service; }

    /** GET /api/routines?date=2026-11-18 (생략 시 오늘) */
    @GetMapping
    public List<RoutineDto.TodayItem> forDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.forDate(date == null ? LocalDate.now() : date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(@Valid @RequestBody RoutineDto.Request req) {
        return Map.of("id", service.create(req));
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @Valid @RequestBody RoutineDto.Request req) { service.update(id, req); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.deactivate(id); }

    /** POST /api/routines/3/toggle?date=2026-11-18 → {"checked": true} */
    @PostMapping("/{id}/toggle")
    public Map<String, Boolean> toggle(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Map.of("checked", service.toggle(id, date == null ? LocalDate.now() : date));
    }
}
