package com.example.david_api.warehouse.controller;

import com.example.david_api.ingestion.repository.StagingClientRepository;
import com.example.david_api.ingestion.repository.StagingProductRepository;
import com.example.david_api.ingestion.repository.StagingSaleLineRepository;
import com.example.david_api.warehouse.repository.DimClientRepository;
import com.example.david_api.warehouse.repository.DimProductRepository;
import com.example.david_api.warehouse.repository.FactSaleRepository;
import com.example.david_api.warehouse.repository.SyncLogRepository;
import com.example.david_api.ingestion.repository.StagingStockRepository;
import com.example.david_api.ingestion.entity.StagingStock;

import com.example.david_api.warehouse.service.WarehouseSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/warehouse")
public class WarehouseController {
    private final WarehouseSyncService warehouseSyncServiceA;
    private final StagingProductRepository stagingProductRepo;
    private final StagingClientRepository stagingClientRepo;
    private final StagingSaleLineRepository stagingSaleLineRepo;
    private final DimProductRepository dimProductRepo;
    private final DimClientRepository dimClientRepo;
    private final FactSaleRepository factSaleRepo;
    private final StagingStockRepository stagingStockRepo;
    private final SyncLogRepository syncLogRepo;

    public WarehouseController(
            WarehouseSyncService warehouseSyncServiceB,
            StagingProductRepository stagingProductRepo,
            StagingClientRepository stagingClientRepo,
            StagingSaleLineRepository stagingSaleLineRepo,
            DimProductRepository dimProductRepo,
            DimClientRepository dimClientRepo,
            FactSaleRepository factSaleRepo,
            StagingStockRepository stagingStockRepo,
            SyncLogRepository syncLogRepo) {
        this.warehouseSyncServiceA = warehouseSyncServiceB;
        this.stagingProductRepo = stagingProductRepo;
        this.stagingClientRepo = stagingClientRepo;
        this.stagingSaleLineRepo = stagingSaleLineRepo;
        this.dimProductRepo = dimProductRepo;
        this.dimClientRepo = dimClientRepo;
        this.factSaleRepo = factSaleRepo;
        this.stagingStockRepo = stagingStockRepo;
        this.syncLogRepo = syncLogRepo;
    }

    @PostMapping("/sync")
    public ResponseEntity<String> triggerSync() {
        warehouseSyncServiceA.sync();
        return ResponseEntity.ok("Warehouse sync completed");
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("staging_products", stagingProductRepo.count());
        status.put("staging_clients", stagingClientRepo.count());
        status.put("staging_sale_lines", stagingSaleLineRepo.count());
        status.put("dim_product", dimProductRepo.count());
        status.put("dim_client", dimClientRepo.count());
        status.put("fact_sale", factSaleRepo.count());
        status.put("stock_last_synced", stagingStockRepo.findTopByOrderBySyncedAtDesc()
                .map(s -> s.getSyncedAt().format(FMT))
                .orElse("no stock data yet"));
        status.put("last_sale_line_received", stagingSaleLineRepo.findTopByOrderBySyncedAtDesc()
                .map(s -> s.getSyncedAt().format(FMT))
                .orElse("no data yet"));
        status.put("last_synced", warehouseSyncServiceA.isSyncing()
                ? "syncing in progress..."
                : syncLogRepo.findById(1L)
                        .map(s -> s.getLastSyncedAt().format(FMT))
                        .orElse("never synced"));

        return ResponseEntity.ok(status);

    }
}