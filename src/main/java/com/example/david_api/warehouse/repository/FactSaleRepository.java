package com.example.david_api.warehouse.repository;

import com.example.david_api.warehouse.analytics.RevenuePeriodRow;
import com.example.david_api.warehouse.analytics.StockForecastRow;
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

        // Revenue grouped by month, as a "YYYY-MM" period label.
        @Query(value = """
                        SELECT TO_CHAR(dd.full_date, 'YYYY-MM') AS period,
                               SUM(fs.total_amount)             AS revenue
                        FROM fact_sale fs
                        JOIN dim_date dd ON fs.date_id = dd.id
                        WHERE dd.full_date BETWEEN :from AND :to
                        GROUP BY TO_CHAR(dd.full_date, 'YYYY-MM')
                        ORDER BY period
                        """, nativeQuery = true)
        List<RevenuePeriodRow> revenuePeriodByMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);

        // Revenue grouped by day, as a "YYYY-MM-DD" period label.
        @Query(value = """
                        SELECT TO_CHAR(dd.full_date, 'YYYY-MM-DD') AS period,
                               SUM(fs.total_amount)                AS revenue
                        FROM fact_sale fs
                        JOIN dim_date dd ON fs.date_id = dd.id
                        WHERE dd.full_date BETWEEN :from AND :to
                        GROUP BY TO_CHAR(dd.full_date, 'YYYY-MM-DD')
                        ORDER BY period
                        """, nativeQuery = true)
        List<RevenuePeriodRow> revenuePeriodByDay(@Param("from") LocalDate from, @Param("to") LocalDate to);

        // Revenue grouped by ISO week, as a "IYYY-Www" period label (e.g. 2026-W03).
        @Query(value = """
                        SELECT TO_CHAR(dd.full_date, 'IYYY-"W"IW') AS period,
                               SUM(fs.total_amount)                AS revenue
                        FROM fact_sale fs
                        JOIN dim_date dd ON fs.date_id = dd.id
                        WHERE dd.full_date BETWEEN :from AND :to
                        GROUP BY TO_CHAR(dd.full_date, 'IYYY-"W"IW')
                        ORDER BY period
                        """, nativeQuery = true)
        List<RevenuePeriodRow> revenuePeriodByWeek(@Param("from") LocalDate from, @Param("to") LocalDate to);

        // Revenue grouped by year, as a "YYYY" period label (e.g. 2026).
        @Query(value = """
                        SELECT TO_CHAR(dd.full_date, 'YYYY') AS period,
                               SUM(fs.total_amount)          AS revenue
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
        @Query(value = """
                        WITH current_stock AS (
                            SELECT item_id, item_name, SUM(quantity) AS current_stock
                            FROM (
                                SELECT DISTINCT ON (item_id, id_lot)
                                       item_id, item_name, id_lot, quantity, synced_at
                                FROM staging_stock
                                ORDER BY item_id, id_lot, synced_at DESC
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
