package com.gooday.dday;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ddays")
public class DDayController {
    private final DDayService service;

    public DDayController(DDayService service) { this.service = service; }

    @GetMapping
    public List<DDayDto.Response> list() { return service.list(LocalDate.now()); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DDayDto.Response create(@Valid @RequestBody DDayDto.Request req) { return service.create(req); }

    @PutMapping("/{id}")
    public DDayDto.Response update(@PathVariable Long id, @Valid @RequestBody DDayDto.Request req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
