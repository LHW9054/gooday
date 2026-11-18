package com.gooday.finance;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 돈 계획표 테이블 접근 (집계는 FinanceService 에서) */
@Repository
public class FinanceDao {
    private final JdbcTemplate jdbc;

    public FinanceDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Item(long id, String kind, String group, String name, String currency,
                       long opening, boolean residual, int sort) {}
    public record AssetGoal(String name, String group, String currency, long target) {}
    public record Debt(long id, String name, long total, String currency) {}
    public record Payment(String ym, long amount) {}

    public List<Item> items(long uid) {
        return jdbc.query("""
                SELECT id, kind, group_name, name, currency, opening_balance, residual, sort_order
                  FROM budget_items WHERE user_id = ? AND active ORDER BY sort_order, id""",
                (rs, i) -> new Item(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getLong(6), rs.getBoolean(7), rs.getInt(8)), uid);
    }

    /** itemId → (ym → 금액), toYm 까지 전부 (누적 계산용) */
    public Map<Long, TreeMap<String, Long>> entriesUpTo(long uid, String toYm) {
        Map<Long, TreeMap<String, Long>> map = new HashMap<>();
        jdbc.query("""
                SELECT e.item_id, e.ym, e.amount FROM budget_entries e
                  JOIN budget_items i ON i.id = e.item_id
                 WHERE i.user_id = ? AND e.ym <= ?""",
                rs -> { map.computeIfAbsent(rs.getLong(1), k -> new TreeMap<>()).put(rs.getString(2), rs.getLong(3)); },
                uid, toYm);
        return map;
    }

    public boolean ownsItem(long uid, long itemId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM budget_items WHERE id = ? AND user_id = ?",
                Integer.class, itemId, uid);
        return n != null && n > 0;
    }

    public void upsertEntry(long itemId, String ym, long amount) {
        jdbc.update("""
                INSERT INTO budget_entries (item_id, ym, amount) VALUES (?, ?, ?) AS n
                ON DUPLICATE KEY UPDATE amount = n.amount""", itemId, ym, amount);
    }

    /** from 달의 금액을 to 달로 복사 (이미 입력한 칸은 유지, 용돈은 자동이라 제외) */
    public int copyMonth(long uid, String from, String to) {
        return jdbc.update("""
                INSERT IGNORE INTO budget_entries (item_id, ym, amount)
                SELECT e.item_id, ?, e.amount FROM budget_entries e
                  JOIN budget_items i ON i.id = e.item_id
                 WHERE i.user_id = ? AND e.ym = ? AND i.active AND NOT i.residual""", to, uid, from);
    }

    public long addItem(long uid, String kind, String group, String name, String currency, long opening) {
        Integer sort = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) + 1 FROM budget_items WHERE user_id = ?", Integer.class, uid);
        jdbc.update("""
                INSERT INTO budget_items (user_id, kind, group_name, name, currency, opening_balance, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?) AS n ON DUPLICATE KEY UPDATE active = TRUE""",
                uid, kind, group, name, currency, opening, sort);
        return jdbc.queryForObject("""
                SELECT id FROM budget_items
                 WHERE user_id = ? AND kind = ? AND group_name = ? AND name = ? AND currency = ?""",
                Long.class, uid, kind, group, name, currency);
    }

    public void deactivateItem(long uid, long id) {
        jdbc.update("UPDATE budget_items SET active = FALSE WHERE id = ? AND user_id = ?", id, uid);
    }

    public List<AssetGoal> assetGoals(long uid, int year) {
        return jdbc.query("""
                SELECT name, saving_group, currency, target FROM asset_goals
                 WHERE user_id = ? AND year = ? ORDER BY sort_order, id""",
                (rs, i) -> new AssetGoal(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4)), uid, year);
    }

    public Map<Integer, Long> quarterGoals(long uid, int year) {
        Map<Integer, Long> m = new TreeMap<>();
        jdbc.query("SELECT quarter, target FROM quarter_goals WHERE user_id = ? AND year = ?",
                rs -> { m.put(rs.getInt(1), rs.getLong(2)); }, uid, year);
        return m;
    }

    public List<Debt> debts(long uid) {
        return jdbc.query("SELECT id, name, total, currency FROM debts WHERE user_id = ? ORDER BY id",
                (rs, i) -> new Debt(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4)), uid);
    }

    public List<Payment> payments(long debtId) {
        return jdbc.query("SELECT ym, amount FROM debt_payments WHERE debt_id = ? ORDER BY ym",
                (rs, i) -> new Payment(rs.getString(1), rs.getLong(2)), debtId);
    }

    public Double fx(long uid, String ym) {
        List<Double> r = jdbc.queryForList("SELECT krw_per_jpy FROM fx_rates WHERE user_id = ? AND ym = ?",
                Double.class, uid, ym);
        return r.isEmpty() ? null : r.get(0);
    }

    public void upsertFx(long uid, String ym, double rate) {
        jdbc.update("""
                INSERT INTO fx_rates (user_id, ym, krw_per_jpy) VALUES (?, ?, ?) AS n
                ON DUPLICATE KEY UPDATE krw_per_jpy = n.krw_per_jpy""", uid, ym, rate);
    }
}
