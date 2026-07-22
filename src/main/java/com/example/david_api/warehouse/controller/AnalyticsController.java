package com.example.david_api.warehouse.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.david_api.warehouse.repository.FactSaleRepository;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final FactSaleRepository factSaleRepo;

    public AnalyticsController(FactSaleRepository factSaleRepo) {
        this.factSaleRepo = factSaleRepo;
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenue(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        var result = factSaleRepo.revenueByMonth(from, to);

        return ResponseEntity.ok(result);
        // call factSaleRepo.revenueByMonth and return the result
    }

    @GetMapping("/top-products")
    public ResponseEntity<?> getTopProducts(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(defaultValue = "5") int limit) {
        var result = factSaleRepo.topProductsByRevenue(from, to, limit);

        return ResponseEntity.ok(result);
        // call factSaleRepo.topProductsByRevenue and
    }
}
