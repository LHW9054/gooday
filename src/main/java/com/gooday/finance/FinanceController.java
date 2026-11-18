package com.gooday.finance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/finance")
public class FinanceController {
    private final FinanceService service;

    public FinanceController(FinanceService service) { this.service = service; }

    /** GET /api/finance/month?ym=2027-04 */
    @GetMapping("/month")
    public FinanceDto.Month month(@RequestParam(required = false) @Pattern(regexp = FinanceDto.YM) String ym) {
        return service.month(ym == null ? YearMonth.now().toString() : ym);
    }

    /** GET /api/finance/year?year=2027 */
    @GetMapping("/year")
    public FinanceDto.Year year(@RequestParam(required = false) Integer year) {
        return service.year(year == null ? LocalDate.now().getYear() : year);
    }

    @PutMapping("/entries")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setEntry(@Valid @RequestBody FinanceDto.EntryRequest req) { service.setEntry(req); }

    /** 지난달 금액을 이번 달로 복사 (비어 있는 칸만) */
    @PostMapping("/month/copy")
    public Map<String, Integer> copy(@RequestParam @Pattern(regexp = FinanceDto.YM) String ym) {
        return Map.of("copied", service.copyFromPrevious(ym));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> addItem(@Valid @RequestBody FinanceDto.ItemRequest req) {
        return Map.of("id", service.addItem(req));
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable long id) { service.removeItem(id); }

    @PutMapping("/fx")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setFx(@Valid @RequestBody FinanceDto.FxRequest req) { service.setFx(req); }
}
