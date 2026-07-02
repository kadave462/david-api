package com.example.david_api.ingestion.controller;

import com.example.david_api.ingestion.service.*;
import com.example.david_api.ingestion.dto.ClientDTO;
import com.example.david_api.ingestion.dto.ProductDTO;
import com.example.david_api.ingestion.dto.SaleDTO;
import com.example.david_api.ingestion.dto.StockDTO;
import com.example.david_api.ingestion.entity.StagingClient;
import com.example.david_api.ingestion.entity.StagingProduct;
import com.example.david_api.ingestion.entity.StagingSale;
import com.example.david_api.ingestion.entity.StagingStock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingest")
public class IngestionController {
    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<StagingProduct>> getProducts() {
        return ResponseEntity.ok(ingestionService.getProducts());
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StagingStock>> getStock() {
        return ResponseEntity.ok(ingestionService.getStock());
    }

    @PostMapping("/stock")
    public ResponseEntity<String> ingestStock(
            @RequestBody List<StockDTO> stockList,
            @RequestHeader("X-Pharmacy-ID") String pharmacyId,
            @RequestHeader("X-Api-Key") String apiKey) {
        ingestionService.saveStock(stockList, pharmacyId, apiKey);
        return ResponseEntity.ok("Stock received: " + stockList.size());
    }

    @GetMapping("/clients")
    public ResponseEntity<List<StagingClient>> getClients() {
        return ResponseEntity.ok(ingestionService.getClients());
    }

    @PostMapping("/clients")
    public ResponseEntity<String> ingestClients(
            @RequestBody List<ClientDTO> clients,
            @RequestHeader("X-Pharmacy-ID") String pharmacyId,
            @RequestHeader("X-Api-Key") String apiKey) {
        ingestionService.saveClients(clients, pharmacyId, apiKey);
        return ResponseEntity.ok("Clients received: " + clients.size());
    }

    @GetMapping("/sales")
    public ResponseEntity<List<StagingSale>> getSales() {
        return ResponseEntity.ok(ingestionService.getSales());
    }

    @PostMapping("/sales")
    public ResponseEntity<String> ingestSales(
            @RequestBody List<SaleDTO> sales,
            @RequestHeader("X-Pharmacy-ID") String pharmacyId,
            @RequestHeader("X-Api-Key") String apiKey) {
        ingestionService.saveSales(sales, pharmacyId, apiKey);
        return ResponseEntity.ok("Sales received: " + sales.size());
    }

    @PostMapping("/products")
    public ResponseEntity<String> ingestProducts(
            @RequestBody List<ProductDTO> products,
            @RequestHeader("X-Pharmacy-ID") String pharmacyId,
            @RequestHeader("X-Api-Key") String apiKey) {

        ingestionService.saveProducts(products, pharmacyId, apiKey);
        return ResponseEntity.ok("Products received: " + products.size());
    
    }

}