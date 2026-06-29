package com.example.david_api.ingestion.controller;

import com.example.david_api.ingestion.service.*;
import com.example.david_api.ingestion.dto.ProductDTO;
import com.example.david_api.ingestion.entity.StagingProduct;
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

    @PostMapping("/products")
    public ResponseEntity<String> ingestProducts(
            @RequestBody List<ProductDTO> products,
            @RequestHeader("X-Pharmacy-ID") String pharmacyId,
            @RequestHeader("X-Api-Key") String apiKey) {

        ingestionService.saveProducts(products, pharmacyId, apiKey);
        return ResponseEntity.ok("Products received: " + products.size());
    
    }

}