package com.gooday.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public final class FinanceDto {
    private FinanceDto() {}

    static final String YM = "\\d{4}-(0[1-9]|1[0-2])";

    /** 한 줄. auto = 계산값(용돈, 투자·저축 합계)이라 직접 입력 불가 */
    public record Line(Long itemId, String name, long amount, Long cumulative, Double percent, boolean auto) {}

    /** 그룹. 저축이면 total = 누적 총저축액, change = 이달성장률 / 지출이면 change = 전달대비 */
    public record Group(String name, String currency, List<Line> lines, long total, Long prevTotal, Double changePct) {}

    public record Month(String ym, List<Group> income, List<Group> expense,
                        List<Group> savingJpy, List<Group> savingKrw,
                        long incomeTotal, long expenseTotal, long savingJpyMonth, long savingKrwMonth,
                        long savingKrwTotal, Double krwPerJpy, boolean empty, List<String> warnings) {}

    public record Row(String group, long[] months, long total) {}
    public record GoalStatus(String name, String currency, long target, long current, long remaining, double percent) {}
    public record QuarterStatus(int quarter, long target, long saved, Double growthPct, double percent) {}
    public record SavingStatus(String group, String name, String currency, long cumulative) {}
    public record DebtStatus(long id, String name, long total, long paid, long remaining, double percent,
                             List<FinanceDao.Payment> payments) {}

    public record Year(int year, List<Row> income, List<Row> expense, long[] incomeTotals, long[] expenseTotals,
                       List<GoalStatus> assets, List<QuarterStatus> quarters, List<SavingStatus> krwSavings,
                       List<DebtStatus> debts, String latestYm) {}

    public record EntryRequest(@NotNull Long itemId, @NotNull @Pattern(regexp = YM) String ym, @NotNull Long amount) {}

    public record ItemRequest(@NotNull @Pattern(regexp = "INCOME|EXPENSE|SAVING") String kind,
                              @NotBlank String group, @NotBlank String name,
                              @Pattern(regexp = "JPY|KRW") String currency, Long opening) {}

    public record FxRequest(@NotNull @Pattern(regexp = YM) String ym, @NotNull Double krwPerJpy) {}
}
