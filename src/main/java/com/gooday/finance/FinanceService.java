package com.gooday.finance;

import com.gooday.common.ApiException;
import com.gooday.common.CurrentUser;
import com.gooday.finance.FinanceDao.Item;
import com.gooday.finance.FinanceDto.*;
import java.time.YearMonth;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * 돈 계획표 계산. Numbers 시트의 수식을 코드로 옮긴 것:
 *  - 그룹 합계, 수입 항목별 퍼센트
 *  - 지출 '투자·저축' = 그달 엔화 저축 합계 (자동)
 *  - 용돈 = 수입 합계 - 다른 지출 합계 (자동, 마이너스면 경고)
 *  - 저축 누적 = 시작잔액 + 지금까지 넣은 돈, 이달성장률 = (이번달 누적 - 지난달 누적) / 지난달 누적
 *  - 전달대비 = (이번달 - 지난달) / 지난달  (지난달이 0이면 표시 안 함)
 */
@Service
public class FinanceService {
    static final String INVEST_GROUP = "투자·저축💹";
    private final FinanceDao dao;

    public FinanceService(FinanceDao dao) { this.dao = dao; }

    /** 한 사용자의 항목 + 금액을 메모리에 올려 두고 계산 */
    private record Ctx(List<Item> items, Map<Long, TreeMap<String, Long>> entries) {
        long amt(Item it, String ym) {
            var m = entries.get(it.id());
            return m == null ? 0 : m.getOrDefault(ym, 0L);
        }
        long cum(Item it, String ym) {
            var m = entries.get(it.id());
            long s = it.opening();
            if (m != null) for (long v : m.headMap(ym, true).values()) s += v;
            return s;
        }
        List<Item> of(String kind) { return items.stream().filter(i -> i.kind().equals(kind)).toList(); }
        List<Item> savings(String cur) { return of("SAVING").stream().filter(i -> i.currency().equals(cur)).toList(); }
        boolean hasData(String ym) {
            return entries.values().stream().anyMatch(m -> m.containsKey(ym));
        }
    }

    private Ctx ctx(String toYm) {
        return new Ctx(dao.items(CurrentUser.ID), dao.entriesUpTo(CurrentUser.ID, toYm));
    }

    // ───────── 월 화면 ─────────
    public Month month(String ym) {
        String prev = YearMonth.parse(ym).minusMonths(1).toString();
        Ctx c = ctx(ym);

        long incomeTotal = c.of("INCOME").stream().mapToLong(i -> c.amt(i, ym)).sum();
        List<Group> income = groups(c.of("INCOME"), it -> {
            long a = c.amt(it, ym);
            return new Line(it.id(), it.name(), a, null, incomeTotal == 0 ? null : round1(a * 100.0 / incomeTotal), false);
        }, null);

        List<Group> expense = expenseGroups(c, ym);
        Map<String, Long> prevTotals = new HashMap<>();
        for (Group g : expenseGroups(c, prev)) prevTotals.put(g.name(), g.total());
        expense = expense.stream().map(g -> {
            Long p = prevTotals.get(g.name());
            return new Group(g.name(), g.currency(), g.lines(), g.total(), p, change(g.total(), p));
        }).toList();
        long expenseTotal = expense.stream().mapToLong(Group::total).sum();

        List<Group> jpy = savingGroups(c, "JPY", ym, prev);
        List<Group> krw = savingGroups(c, "KRW", ym, prev);

        List<String> warnings = new ArrayList<>();
        expense.stream().flatMap(g -> g.lines().stream())
                .filter(l -> l.auto() && l.amount() < 0)
                .forEach(l -> warnings.add(l.name() + "이(가) " + l.amount() + "엔 이에요. 지출이 수입보다 많아요."));
        if (expense.stream().flatMap(g -> g.lines().stream()).noneMatch(Line::auto) && incomeTotal != expenseTotal)
            warnings.add("수입과 지출 합계가 " + (incomeTotal - expenseTotal) + "엔 차이 나요.");

        return new Month(ym, income, expense, jpy, krw, incomeTotal, expenseTotal,
                c.savings("JPY").stream().mapToLong(i -> c.amt(i, ym)).sum(),
                c.savings("KRW").stream().mapToLong(i -> c.amt(i, ym)).sum(),
                c.savings("KRW").stream().mapToLong(i -> c.cum(i, ym)).sum(),
                dao.fx(CurrentUser.ID, ym), !c.hasData(ym), warnings);
    }

    /** 지출: 맨 앞에 '투자·저축'(엔화 저축 합계, 자동) + 입력 항목들 + 용돈(나머지, 자동) */
    private List<Group> expenseGroups(Ctx c, String ym) {
        long invest = c.savings("JPY").stream().mapToLong(i -> c.amt(i, ym)).sum();
        long income = c.of("INCOME").stream().mapToLong(i -> c.amt(i, ym)).sum();
        long others = invest + c.of("EXPENSE").stream().filter(i -> !i.residual()).mapToLong(i -> c.amt(i, ym)).sum();
        boolean[] residualUsed = {false};

        List<Group> list = new ArrayList<>();
        list.add(new Group(INVEST_GROUP, "JPY",
                List.of(new Line(null, "엔화 저축 합계", invest, null, null, true)), invest, null, null));
        list.addAll(groups(c.of("EXPENSE"), it -> {
            if (it.residual() && !residualUsed[0]) {
                residualUsed[0] = true;
                return new Line(it.id(), it.name(), income - others, null, null, true);
            }
            return new Line(it.id(), it.name(), c.amt(it, ym), null, null, false);
        }, null));
        return list;
    }

    private List<Group> savingGroups(Ctx c, String cur, String ym, String prev) {
        return groups(c.savings(cur), it -> new Line(it.id(), it.name(), c.amt(it, ym), c.cum(it, ym), null, false),
                g -> {
                    long now = g.lines().stream().mapToLong(Line::cumulative).sum();
                    long before = c.savings(cur).stream().filter(i -> i.group().equals(g.name()))
                            .mapToLong(i -> c.cum(i, prev)).sum();
                    return new Group(g.name(), cur, g.lines(), now, before, change(now, before));
                });
    }

    /** 항목을 group_name 순서대로 묶기 */
    private List<Group> groups(List<Item> items, java.util.function.Function<Item, Line> line,
                               java.util.function.UnaryOperator<Group> post) {
        Map<String, List<Line>> m = new LinkedHashMap<>();
        Map<String, String> cur = new HashMap<>();
        for (Item it : items) {
            m.computeIfAbsent(it.group(), k -> new ArrayList<>()).add(line.apply(it));
            cur.putIfAbsent(it.group(), it.currency());
        }
        List<Group> out = new ArrayList<>();
        m.forEach((name, lines) -> {
            Group g = new Group(name, cur.get(name), lines, lines.stream().mapToLong(Line::amount).sum(), null, null);
            out.add(post == null ? g : post.apply(g));
        });
        return out;
    }

    // ───────── 연간 종합표 ─────────
    public Year year(int year) {
        String dec = year + "-12";
        Ctx c = ctx(dec);
        List<String> yms = new ArrayList<>();
        for (int m = 1; m <= 12; m++) yms.add(String.format("%d-%02d", year, m));

        Map<String, long[]> inc = new LinkedHashMap<>(), exp = new LinkedHashMap<>();
        long[] incT = new long[12], expT = new long[12];
        String latest = null;
        for (int m = 0; m < 12; m++) {
            String ym = yms.get(m);
            boolean has = c.hasData(ym);
            if (has) latest = ym;
            Month mo = has ? month(ym) : null;
            for (Group g : has ? mo.income() : groups(c.of("INCOME"), it -> new Line(null, "", 0, null, null, false), null)) {
                inc.computeIfAbsent(g.name(), k -> new long[12])[m] = g.total();
                incT[m] += g.total();
            }
            for (Group g : has ? mo.expense() : expenseGroups(c, "0000-00")) {
                exp.computeIfAbsent(g.name(), k -> new long[12])[m] = has ? g.total() : 0;
                expT[m] += has ? g.total() : 0;
            }
        }
        String asOf = latest == null ? dec : latest;

        List<GoalStatus> assets = dao.assetGoals(CurrentUser.ID, year).stream().map(g -> {
            long cur = c.of("SAVING").stream()
                    .filter(i -> i.group().equals(g.group()) && i.currency().equals(g.currency()))
                    .mapToLong(i -> c.cum(i, asOf)).sum();
            return new GoalStatus(g.name(), g.currency(), g.target(), cur, Math.max(0, g.target() - cur),
                    pct(cur, g.target()));
        }).toList();

        List<QuarterStatus> quarters = new ArrayList<>();
        Long prevSaved = null;
        for (var e : dao.quarterGoals(CurrentUser.ID, year).entrySet()) {
            int q = e.getKey();
            long saved = 0;
            for (int m = (q - 1) * 3; m < q * 3; m++) {
                String ym = yms.get(m);
                saved += c.savings("JPY").stream().mapToLong(i -> c.amt(i, ym)).sum();
            }
            // 아직 시작 안 한 분기는 성장률을 계산하지 않음
            boolean started = latest != null && yms.get((q - 1) * 3).compareTo(latest) <= 0;
            quarters.add(new QuarterStatus(q, e.getValue(), saved, started ? change(saved, prevSaved) : null,
                    pct(saved, e.getValue())));
            prevSaved = saved;
        }

        List<SavingStatus> krw = c.savings("KRW").stream()
                .map(i -> new SavingStatus(i.group(), i.name(), "KRW", c.cum(i, asOf))).toList();

        List<DebtStatus> debts = dao.debts(CurrentUser.ID).stream().map(d -> {
            var pays = dao.payments(d.id());
            long paid = pays.stream().mapToLong(FinanceDao.Payment::amount).sum();
            return new DebtStatus(d.id(), d.name(), d.total(), paid, Math.max(0, d.total() - paid), pct(paid, d.total()), pays);
        }).toList();

        return new Year(year, rows(inc), rows(exp), incT, expT, assets, quarters, krw, debts, latest);
    }

    // ───────── 입력 ─────────
    public void setEntry(EntryRequest r) {
        if (!dao.ownsItem(CurrentUser.ID, r.itemId())) throw ApiException.notFound("항목");
        dao.upsertEntry(r.itemId(), r.ym(), r.amount());
    }

    public int copyFromPrevious(String ym) {
        return dao.copyMonth(CurrentUser.ID, YearMonth.parse(ym).minusMonths(1).toString(), ym);
    }

    public long addItem(ItemRequest r) {
        return dao.addItem(CurrentUser.ID, r.kind(), r.group().trim(), r.name().trim(),
                r.currency() == null ? "JPY" : r.currency(), r.opening() == null ? 0 : r.opening());
    }

    public void removeItem(long id) { dao.deactivateItem(CurrentUser.ID, id); }

    public void setFx(FxRequest r) { dao.upsertFx(CurrentUser.ID, r.ym(), r.krwPerJpy()); }

    // ───────── util ─────────
    private static List<Row> rows(Map<String, long[]> m) {
        List<Row> out = new ArrayList<>();
        m.forEach((k, v) -> out.add(new Row(k, v, Arrays.stream(v).sum())));
        return out;
    }

    private static Double change(long now, Long before) {
        return before == null || before == 0 ? null : round1((now - before) * 100.0 / before);
    }

    private static double pct(long a, long b) { return b == 0 ? 0 : round1(a * 100.0 / b); }

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }
}
