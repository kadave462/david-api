package com.example.david_api.warehouse.repository;

import com.example.david_api.warehouse.analytics.RevenueRow;
import com.example.david_api.warehouse.analytics.TopProductRow;
import com.example.david_api.warehouse.entity.FactSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FactSaleRepository extends JpaRepository<FactSale, Long> {
    boolean existsBySourceSaleLineId(Long sourceSaleLineId);

    Optional<FactSale> findTopByOrderBySourceSaleLineIdDesc();

    // query to get revenue by month between two dates
    @Query(value = "SELECT dd.year, dd.month, SUM(fs.total_amount) AS revenue\r\n" + //
            "FROM fact_sale fs\r\n" + //
            "JOIN dim_date dd ON fs.date_id = dd.id\r\n" + //
            "WHERE dd.full_date BETWEEN :from AND :to\r\n" + //
            "GROUP BY dd.year, dd.month\r\n" + //
            "ORDER BY dd.year, dd.month", nativeQuery = true)
    List<RevenueRow> revenueByMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // query to get top 5 products by revenue between two dates
    @Query(value = "SELECT dp.product_name, SUM(fs.total_amount) AS total_revenue, SUM(fs.quantity) AS total_quantity\r\n" + //
            "FROM fact_sale fs\r\n" + //
            "JOIN dim_product dp ON fs.product_id = dp.id\r\n" + //
            "JOIN dim_date dd ON fs.date_id = dd.id\r\n" + //
            "WHERE dd.full_date BETWEEN :from AND :to\r\n" + //
            "GROUP BY dp.product_name\r\n" + //
            "ORDER BY SUM(fs.total_amount) DESC\r\n" + //
            "LIMIT :limit", nativeQuery = true)
            List<TopProductRow> topProductsByRevenue(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("limit") int limit);

}
