package com.gooday.goal;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goals")
public class GoalController {
    private final GoalService service;

    public GoalController(GoalService service) { this.service = service; }

    @GetMapping
    public List<GoalDto.Status> list() { return service.statuses(LocalDate.now()); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(@Valid @RequestBody GoalDto.Request req) {
        return Map.of("id", service.create(req));
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @Valid @RequestBody GoalDto.Request req) { service.update(id, req); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.deactivate(id); }

    @PostMapping("/{id}/progress")
    public GoalDto.Status addProgress(@PathVariable Long id, @Valid @RequestBody GoalDto.ProgressRequest req) {
        return service.addProgress(id, req);
    }
}
