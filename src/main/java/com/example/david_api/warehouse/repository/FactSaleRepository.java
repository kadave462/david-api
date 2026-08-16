package com.example.david_api.warehouse.repository;

import com.example.david_api.warehouse.analytics.RevenuePeriodRow;
import com.example.david_api.warehouse.analytics.StockForecastRow;
import com.example.david_api.warehouse.analytics.TopMoverRow;
import com.example.david_api.warehouse.analytics.TopPayerRow;
import com.example.david_api.warehouse.analytics.TopProductRow;
import com.example.david_api.warehouse.entity.FactSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Repository for the fact_sale table. Extending JpaRepository gives us the basic
// CRUD methods (save, findById, findAll, count...) for free. Below we add:
//   - a few Spring-generated finder methods (name → SQL, no @Query needed)
//   - custom analytics queries written by hand with @Query + native SQL,
//     each returning a projection (an interface that catches the SELECT shape).
public interface FactSaleRepository extends JpaRepository<FactSale, Long> {

        // Dedup guard used by the ETL: "is this source sale line already in fact_sale?"
        // syncFacts() calls this before inserting so the same line is never added twice.
        boolean existsBySourceSaleLineId(Long sourceSaleLineId);

        // Returns the fact row with the highest source_sale_line_id (the newest line
        // processed). Used by the bootstrap check in syncFacts() to compare against staging.
        Optional<FactSale> findTopByOrderBySourceSaleLineIdDesc();

        // Raw fact_sale rows (not aggregated) for a date range — the equivalent of
        // GET /api/v1/ingest/stock, but for sales. Bounded by :from/:to rather than
        // a plain findAll(): unlike staging_stock (~3k rows), fact_sale has been
        // growing continuously since the 2026-01 backfill and an unbounded dump
        // would only get bigger every day.
        @Query(value = """
                        SELECT fs.* FROM fact_sale fs
                        JOIN dim_date dd ON fs.date_id = dd.id
                        WHERE dd.full_date BETWEEN :from AND :to
                        ORDER BY fs.invoice_time
                        """, nativeQuery = true)
        List<FactSale> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

        // ── ANALYTICS QUERIES ─────────────────────────────────────────────
        // All of these: filter fact_sale by a date range (via dim_date), aggregate,
        // and return a projection. nativeQuery = true means the SQL is raw Postgres
        // (real table/column names), not JPQL. The AS aliases must match the
        // projection getters (e.g. AS revenue → getRevenue()).

        // Top products by revenue, between two dates.
        // Joins dim_product for the name, groups by product, orders by total revenue,
        // and caps the result with LIMIT :limit. total_revenue = SUM(total_amount),
        // total_quantity = SUM(quantity).
        @Query(value = "SELECT dp.product_name, SUM(fs.total_amount) AS total_revenue, SUM(fs.quantity) AS total_quantity\r\n"
                        + //
                        "FROM fact_sale fs\r\n" + //
                        "JOIN dim_product dp ON fs.product_id = dp.id\r\n" + //
                        "JOIN dim_date dd ON fs.date_id = dd.id\r\n" + //
                        "WHERE dd.full_date BETWEEN :from AND :to\r\n" + //
                        "GROUP BY dp.product_name\r\n" + //
                        "ORDER BY SUM(fs.total_amount) DESC\r\n" + //
                        "LIMIT :limit", nativeQuery = true)
        List<TopProductRow> topProductsByRevenue(@Param("from") LocalDate from, @Param("to") LocalDate to,
                        @Param("limit") int limit);

        // Top movers by UNITS SOLD, not revenue — a different ranking than
        // topProductsByRevenue above (a cheap, high-volume item can move the
        // most units without being a top-revenue product, and vice versa).
        // Same cost/profit computation as the revenue-period queries, so this
        // one table can show price, revenue, AND profit per product, not just
        // the ranking metric (quantity). Also joins each product's CURRENT
        // stock lot (from staging_stock, a live snapshot).
        //
        // Deliberately NOT scoped to a :from/:to range like the other
        // analytics queries — it used to be, but that meant a product with
        // real sales history could vanish from this table entirely just
        // because none of its sales fell inside whatever date range happened
        // to be selected on the dashboard (which defaults to a short window
        // meant for the Revenue chart, not this one). "Top movers, ever" is
        // a more stable, coherent question than "top movers in whatever
        // range is currently picked" — so this always reflects full history.
        //
        // latest_lot: staging_stock has one row per (item, lot) sync, and a
        // product can have SEVERAL open lots at once (an old batch + a newly
        // delivered one). Getting this right takes two steps:
        //   1. latest_per_lot — for each (item, BATCH, id_lot) triple, keep
        //      only its most recent sync (a lot gets re-synced repeatedly as
        //      it depletes, so there are many rows per lot over time; we
        //      want its latest state, not its history). id_lot is part of
        //      the grouping key too, not just batch_number: suppliers reuse
        //      batch codes across genuinely different deliveries (same
        //      batch_number, different received date and initial_quantity),
        //      so batch_number alone can collide two distinct physical lots
        //      into one — silently dropping whichever synced less recently.
        //   2. latest_lot — THEN sum quantity/initial_quantity across all of
        //      a product's lots, so a product with 2 open lots reports its
        //      TOTAL stock, not just whichever lot happened to sync last.
        //      id_lot/batch_number/expiration_date can't be summed (they're
        //      text, not numbers) — one lot has to be picked to represent
        //      them (batch_number/id_lot/expiration_date), plus a second
        //      lot for the "what's coming in behind it" columns:
        //
        //      MAIN columns (id_lot/expiration_date/batch_number) = the
        //      OLDEST live lot, if any lot is live — the one actually being
        //      sold down right now, matching FIFO (oldest stock sells
        //      first), and what the depletion-rate/days-remaining math is
        //      about. If NONE are live, falls back to the NEWEST dead lot
        //      that was never followed by a later delivery — i.e. the lot
        //      that was actually in use right up until the product went
        //      quiet, not an arbitrary or ancient one. (id_lot sorts
        //      correctly as a plain string here because it's a fixed
        //      6-digit YYMMDD code.)
        //
        //      NEWEST_* columns = the NEWEST live lot — purely informational
        //      ("here's what's coming in behind the one being sold down"),
        //      null whenever nothing is live (no live lot to report). When
        //      only one lot is live, both the main and NEWEST_* columns
        //      point at that same lot.
        //
        //      initial_quantity is summed over LIVE lots only (quantity > 0),
        //      not every lot the product has ever had. Confirmed on ONGUENT
        //      EMULSIFIANT (item 2439): 43 lots on record, 41 long dead, but
        //      summing initial_quantity across all of them (145) against a
        //      lot documented that same day made the depletion-rate formula
        //      read "139 units sold in 1 day" — a multi-year cumulative total
        //      divided by one day, purely because the representative lot was
        //      brand new. Restricting the sum to live lots (initial 7, live
        //      6) keeps initial_quantity and quantity describing the same
        //      set of lots the depletion rate is actually about. When NONE
        //      are live (quantity_all = 0), initial_quantity is reported as
        //      0 too — not the full historical sum — so the row reads as a
        //      clean "out of stock" (0/0) rather than a fabricated
        //      percentage against lots that no longer exist on the shelf;
        //      the frontend treats initial=0 as its own explicit "Out of
        //      Stock" state rather than computing a depletion rate from it.
        @Query(value = """
                        WITH latest_per_lot AS (
                            SELECT DISTINCT ON (item_id, batch_number, id_lot)
                                   item_id, id_lot, batch_number, initial_quantity, quantity, expiration_date, synced_at
                            FROM staging_stock
                            ORDER BY item_id, batch_number, id_lot, synced_at DESC
                        ),
                        latest_lot AS (
                            SELECT item_id,
                                   CASE WHEN SUM(quantity) > 0
                                        THEN SUM(initial_quantity) FILTER (WHERE quantity > 0)
                                        ELSE 0
                                   END                                                                                                         AS initial_quantity,
                                   SUM(quantity)                                                                                              AS quantity,
                                   (ARRAY_AGG(batch_number ORDER BY (quantity > 0) DESC,
                                              CASE WHEN quantity > 0 THEN id_lot END ASC,
                                              CASE WHEN quantity <= 0 THEN id_lot END DESC))[1]  AS batch_number,
                                   (ARRAY_AGG(id_lot       ORDER BY (quantity > 0) DESC,
                                              CASE WHEN quantity > 0 THEN id_lot END ASC,
                                              CASE WHEN quantity <= 0 THEN id_lot END DESC))[1]  AS id_lot,
                                   (ARRAY_AGG(expiration_date ORDER BY (quantity > 0) DESC,
                                              CASE WHEN quantity > 0 THEN id_lot END ASC,
                                              CASE WHEN quantity <= 0 THEN id_lot END DESC))[1]  AS expiration_date,
                                   CASE WHEN SUM(quantity) > 0
                                        THEN (ARRAY_AGG(id_lot ORDER BY (quantity > 0) DESC, id_lot DESC))[1]
                                        ELSE NULL
                                   END                                                                                                         AS newest_id_lot,
                                   CASE WHEN SUM(quantity) > 0
                                        THEN (ARRAY_AGG(expiration_date ORDER BY (quantity > 0) DESC, id_lot DESC))[1]
                                        ELSE NULL
                                   END                                                                                                         AS newest_expiration_date
                            FROM latest_per_lot
                            GROUP BY item_id
                        )
                        SELECT dp.source_product_id                      AS item_id,
                               dp.product_name                          AS product_name,
                               SUM(fs.quantity)                          AS total_quantity,
                               SUM(fs.total_amount)                      AS total_revenue,
                               SUM(fs.cost_price * fs.quantity)          AS cost,
                               SUM(fs.total_amount) - SUM(fs.cost_price * fs.quantity) AS profit,
                               ll.initial_quantity                       AS initial_quantity,
                               ll.batch_number                           AS batch_number,
                               ll.id_lot                                 AS id_lot,
                               ll.quantity                               AS live_quantity,
                               ll.expiration_date                        AS expiration_date,
                               ll.newest_id_lot                          AS newest_id_lot,
                               ll.newest_expiration_date                 AS newest_expiration_date,
                               MAX(fs.invoice_time)                      AS last_sale
                        FROM fact_sale fs
                        JOIN dim_product dp ON fs.product_id = dp.id
                        LEFT JOIN latest_lot ll ON ll.item_id = dp.source_product_id
                        GROUP BY dp.source_product_id, dp.product_name, ll.initial_quantity, ll.batch_number, ll.id_lot, ll.quantity, ll.expiration_date, ll.newest_id_lot, ll.newest_expiration_date
                        ORDER BY SUM(fs.quantity) DESC
                        LIMIT :limit
                        """, nativeQuery = true)
        List<TopMoverRow> topProductsByQuantity(@Param("limit") int limit);

        // Top payers by revenue, between two dates. This is the "Top Clients" feature,
        // redefined: the invoice records the payer (insurer), never the individual, so
        // we group by fact_sale.insurance directly — a degenerate dimension, no JOIN to
        // dim_client needed. TRIM() collapses trailing-space duplicates ("SANLAM " vs
        // "SANLAM"); COUNT(DISTINCT source_invoice_id) counts orders, not sale lines.
        @Query(value = """
                        SELECT TRIM(fs.insurance)                    AS insurance,
                               SUM(fs.total_amount)                  AS total_revenue,
                               COUNT(DISTINCT fs.source_invoice_id)  AS total_orders
                        FROM fact_sale fs
                        JOIN dim_date dd ON fs.date_id = dd.id
                        WHERE dd.full_date BETWEEN :from AND :to
                        GROUP BY TRIM(fs.insurance)
                        ORDER BY SUM(fs.total_amount) DESC
                        LIMIT :limit
                        """, nativeQuery = true)
        List<TopPayerRow> topPayers(@Param("from") LocalDate from,
                        @Param("to") LocalDate to,
                        @Param("limit") int limit);

        // Revenue grouped by month, as a "YYYY-MM" period label. cost is
        // SUM(cost_price * quantity) — cost_price is per-unit, so it has to be
        // multiplied by quantity before summing, unlike total_amount which is
        // already a line total. profit = revenue - cost, computed in the same
        // SELECT so the two SUMs behind it aren't calculated twice.
        @Query(value = """
                        SELECT TO_CHAR(dd.full_date, 'YYYY-MM') AS period,
                               SUM(fs.total_amount)             AS revenue,
                               SUM(fs.cost_price * fs.quantity) AS cost,
                               SUM(fs.total_amount) - SUM(fs.cost_price * fs.quantity) AS profit
                        FROM fact_sale fs
                        JOIN dim_date dd ON fs.date_id = dd.id
                        WHERE dd.full_date BETWEEN :from AND :to
                        GROUP BY TO_CHAR(dd.full_date, 'YYYY-MM')
                        ORDER BY period
                        """, nativeQuery = true)
        List<RevenuePeriodRow> revenuePeriodByMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);

        // Same shape as revenuePeriodByMonth, but each month only counts days
        // 1 through day-of-month(:to) — e.g. if :to is the 9th, every month
        // in range is capped to its own 1st-9th, not the whole month. Lets
        // "how much had we made by this point" be compared fairly month over
        // month, instead of earlier months (fully counted) looking bigger
        // than the current, still-in-progress one just because it's not over.
        @Query(value = """
                        SELECT TO_CHAR(dd.full_date, 'YYYY-MM') AS period,
                               SUM(fs.total_amount)             AS revenue,
                               SUM(fs.cost_price * fs.quantity) AS cost,
                               SUM(fs.total_amount) - SUM(fs.cost_price * fs.quantity) AS profit
                        FROM fact_sale fs
                        JOIN dim_date dd ON fs.date_id = dd.id
                        WHERE dd.full_date BETWEEN :from AND :to
                          AND EXTRACT(DAY FROM dd.full_date) <= EXTRACT(DAY FROM CAST(:to AS date))
                        GROUP BY TO_CHAR(dd.full_date, 'YYYY-MM')
                        ORDER BY period
                        """, nativeQuery = true)
        List<RevenuePeriodRow> revenuePeriodByMonthToDate(@Param("from") LocalDate from, @Param("to") LocalDate to);

        // Revenue grouped by day, as a "YYYY-MM-DD" period label. Same cost/profit
        // addition as the month query above.
        @Query(value = """
                        SELECT TO_CHAR(dd.full_date, 'YYYY-MM-DD') AS period,
                               SUM(fs.total_amount)                AS revenue,
                               SUM(fs.cost_price * fs.quantity)    AS cost,
                               SUM(fs.total_amount) - SUM(fs.cost_price * fs.quantity) AS profit
                        FROM fact_sale fs
                        JOIN dim_date dd ON fs.date_id = dd.id
                        WHERE dd.full_date BETWEEN :from AND :to
                        GROUP BY TO_CHAR(dd.full_date, 'YYYY-MM-DD')
                        ORDER BY period
                        """, nativeQuery = true)
        List<RevenuePeriodRow> revenuePeriodByDay(@Param("from") LocalDate from, @Param("to") LocalDate to);

        // Revenue grouped by ISO week, as a "IYYY-Www" period label (e.g. 2026-W03).
        // Same cost/profit addition.
        @Query(value = """
                        SELECT TO_CHAR(dd.full_date, 'IYYY-"W"IW') AS period,
                               SUM(fs.total_amount)                AS revenue,
                               SUM(fs.cost_price * fs.quantity)    AS cost,
                               SUM(fs.total_amount) - SUM(fs.cost_price * fs.quantity) AS profit
                        FROM fact_sale fs
                        JOIN dim_date dd ON fs.date_id = dd.id
                        WHERE dd.full_date BETWEEN :from AND :to
                        GROUP BY TO_CHAR(dd.full_date, 'IYYY-"W"IW')
                        ORDER BY period
                        """, nativeQuery = true)
        List<RevenuePeriodRow> revenuePeriodByWeek(@Param("from") LocalDate from, @Param("to") LocalDate to);

        // Revenue grouped by year, as a "YYYY" period label (e.g. 2026).
        // Same cost/profit addition.
        @Query(value = """
                        SELECT TO_CHAR(dd.full_date, 'YYYY') AS period,
                               SUM(fs.total_amount)          AS revenue,
                               SUM(fs.cost_price * fs.quantity) AS cost,
                               SUM(fs.total_amount) - SUM(fs.cost_price * fs.quantity) AS profit
                        FROM fact_sale fs
                        JOIN dim_date dd ON fs.date_id = dd.id
                        WHERE dd.full_date BETWEEN :from AND :to
                        GROUP BY TO_CHAR(dd.full_date, 'YYYY')
                        ORDER BY period
                        """, nativeQuery = true)
        List<RevenuePeriodRow> revenuePeriodByYear(@Param("from") LocalDate from, @Param("to") LocalDate to);


        // Forecasting: average daily sales per product, and days of stock remaining.
        // :days appears twice — once for the division, once for the window cutoff.
        // INTERVAL can't take a bound parameter directly inside the quoted literal,
        // so we multiply an interval by :days instead of writing INTERVAL ':days days'.
        //
        // current_stock already summed across all of a product's lots (that part
        // was always right) — but it used to tell lots apart by id_lot, which
        // turned out to just hold a date, not a real lot identifier. Fixed to
        // use batch_number instead, same correction as topProductsByQuantity above.
        @Query(value = """
                        WITH current_stock AS (
                            SELECT item_id, item_name, SUM(quantity) AS current_stock
                            FROM (
                                SELECT DISTINCT ON (item_id, batch_number, id_lot)
                                       item_id, item_name, batch_number, quantity, synced_at
                                FROM staging_stock
                                ORDER BY item_id, batch_number, id_lot, synced_at DESC
                            ) latest_lots
                            GROUP BY item_id, item_name
                        ),

                        sales_rate AS (
                            SELECT
                                dp.source_product_id           AS item_id,
                                dp.product_name,
                                SUM(fs.quantity)::numeric / :days AS avg_daily_sales
                            FROM fact_sale fs
                            JOIN dim_product dp ON fs.product_id = dp.id
                            JOIN dim_date dd    ON fs.date_id   = dd.id
                            WHERE dd.full_date >= CURRENT_DATE - (:days * INTERVAL '1 day')
                            GROUP BY dp.source_product_id, dp.product_name
                        )

                        SELECT
                            cs.item_id,
                            cs.item_name,
                            cs.current_stock,
                            sr.avg_daily_sales,
                            cs.current_stock / NULLIF(sr.avg_daily_sales, 0) AS days_remaining
                        FROM current_stock cs
                        LEFT JOIN sales_rate sr ON sr.item_id = cs.item_id
                        ORDER BY days_remaining ASC NULLS LAST
                        """, nativeQuery = true)
        List<StockForecastRow> stockForecast(@Param("days") int days);

}
