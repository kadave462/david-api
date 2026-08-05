package com.example.david_api.warehouse.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.david_api.warehouse.repository.FactSaleRepository;

// Read-only analytics endpoints. Each reads a date range from the query string,
// calls a projection query on FactSaleRepository, and returns the list as JSON.
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final FactSaleRepository factSaleRepo;

    public AnalyticsController(FactSaleRepository factSaleRepo) {
        this.factSaleRepo = factSaleRepo;
    }

    // Revenue over a date range, grouped by a chosen period.
    // groupBy picks the granularity; it defaults to "month" so ?groupBy= can be omitted.
    // Each period query returns rows shaped as RevenuePeriodRow { period, revenue }.
    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenue(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "month") String groupBy) {

        var result = switch (groupBy) {
            case "day"  -> factSaleRepo.revenuePeriodByDay(from, to);
            case "week" -> factSaleRepo.revenuePeriodByWeek(from, to);
            case "year" -> factSaleRepo.revenuePeriodByYear(from, to);
            default     -> factSaleRepo.revenuePeriodByMonth(from, to);  // "month" or anything unknown
        };

        return ResponseEntity.ok(result);
    }

    // Top products by revenue over a date range (limit defaults to 5).
    @GetMapping("/top-products")
    public ResponseEntity<?> getTopProducts(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {

        var result = factSaleRepo.topProductsByRevenue(from, to, limit);
        return ResponseEntity.ok(result);
    }

    // Top products by UNITS SOLD over a date range (limit defaults to 50).
    // A different ranking than /top-products, which sorts by revenue —
    // a cheap, high-volume item can top this list without being a top
    // earner. Includes cost/profit per product alongside quantity.
    @GetMapping("/top-movers")
    public ResponseEntity<?> getTopMovers(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "50") int limit) {

        var result = factSaleRepo.topProductsByQuantity(from, to, limit);
        return ResponseEntity.ok(result);
    }

    // Top payers (insurers) by revenue over a date range (limit defaults to 5).
    @GetMapping("/top-payers")
    public ResponseEntity<?> getTopPayers(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {

        var result = factSaleRepo.topPayers(from, to, limit);
        return ResponseEntity.ok(result);
    }

    // Stock depletion forecast: current stock, average daily sales, and days
    // remaining per product. days sets the sales-rate window (defaults to 30).
    @GetMapping("/stock-forecast")
    public ResponseEntity<?> getStockForecast(
            @RequestParam(defaultValue = "30") int days) {

        var result = factSaleRepo.stockForecast(days);
        return ResponseEntity.ok(result);
    }

}