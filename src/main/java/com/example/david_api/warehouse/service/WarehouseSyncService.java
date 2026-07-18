package com.example.david_api.warehouse.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.example.david_api.ingestion.entity.StagingClient;
import com.example.david_api.ingestion.entity.StagingProduct;
import com.example.david_api.ingestion.entity.StagingSaleLine;
import com.example.david_api.ingestion.repository.StagingClientRepository;
import com.example.david_api.ingestion.repository.StagingProductRepository;
import com.example.david_api.ingestion.repository.StagingSaleLineRepository;
import com.example.david_api.ingestion.repository.StagingSaleRepository;
import com.example.david_api.warehouse.entity.DimClient;
import com.example.david_api.warehouse.entity.DimPharmacy;
import com.example.david_api.warehouse.entity.DimProduct;
import com.example.david_api.warehouse.repository.DimClientRepository;
import com.example.david_api.warehouse.repository.DimDateRepository;
import com.example.david_api.warehouse.repository.DimPharmacyRepository;
import com.example.david_api.warehouse.repository.DimProductRepository;
import com.example.david_api.warehouse.entity.DimDate;
import com.example.david_api.warehouse.entity.FactSale;
import com.example.david_api.warehouse.entity.SyncLog;
import com.example.david_api.warehouse.repository.FactSaleRepository;
import com.example.david_api.warehouse.repository.SyncLogRepository;
import com.example.david_api.ingestion.entity.StagingSale;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WarehouseSyncService {
    private final StagingProductRepository stagingProductRepo;
    private final StagingClientRepository stagingClientRepo;
    private final StagingSaleRepository stagingSaleRepo;
    private final StagingSaleLineRepository stagingSaleLineRepo;

    private final DimPharmacyRepository dimPharmacyRepo;
    private final DimDateRepository dimDateRepo;
    private final DimClientRepository dimClientRepo;
    private final DimProductRepository dimProductRepo;
    private final FactSaleRepository factSaleRepo;
    private final SyncLogRepository syncLogRepo;
    private boolean syncing = false;

    public WarehouseSyncService(
            StagingProductRepository stagingProductRepo,
            StagingClientRepository stagingClientRepo,
            StagingSaleRepository stagingSaleRepo,
            StagingSaleLineRepository stagingSaleLineRepo,
            DimPharmacyRepository dimPharmacyRepo,
            DimDateRepository dimDateRepo,
            DimClientRepository dimClientRepo,
            DimProductRepository dimProductRepo,
            FactSaleRepository factSaleRepo,
            SyncLogRepository syncLogRepo) {
        this.stagingProductRepo = stagingProductRepo;
        this.stagingClientRepo = stagingClientRepo;
        this.stagingSaleRepo = stagingSaleRepo;
        this.stagingSaleLineRepo = stagingSaleLineRepo;
        this.dimPharmacyRepo = dimPharmacyRepo;
        this.dimDateRepo = dimDateRepo;
        this.dimClientRepo = dimClientRepo;
        this.dimProductRepo = dimProductRepo;
        this.factSaleRepo = factSaleRepo;
        this.syncLogRepo = syncLogRepo;
    }

    public void syncPharmacies() {
        List<StagingProduct> products = stagingProductRepo.findAll();
        for (StagingProduct product : products) {
            String pharmacyId = product.getPharmacyId();
            if (dimPharmacyRepo.findByPharmacyId(pharmacyId).isEmpty()) {
                DimPharmacy dim = new DimPharmacy();
                dim.setPharmacyId(pharmacyId);
                dimPharmacyRepo.save(dim);
            }
        }
    }

    public void syncProducts() {
        List<StagingProduct> products = stagingProductRepo.findAll();
        for (StagingProduct product : products) {
            Integer sourceProductId = product.getSourceProductId();
            String pharmacyId = product.getPharmacyId();
            if (dimProductRepo.findBySourceProductIdAndPharmacyId(sourceProductId, pharmacyId).isEmpty()) {
                DimProduct dim = new DimProduct();
                dim.setSourceProductId(sourceProductId);
                dim.setPharmacyId(pharmacyId);
                dim.setProductName(product.getProductName());
                dim.setProductCode(product.getProductCode());
                dim.setBarcode(product.getBarcode());
                dim.setUnitPrice(product.getUnitPrice());
                dim.setCostPrice(product.getCostPrice());
                dim.setTvaRate(product.getTvaRate());
                dim.setFamily(product.getFamily());
                dim.setSourceLastUpdated(product.getSourceLastUpdated());
                dimProductRepo.save(dim);
            }
        }

    }

    public void syncClients() {
        List<StagingClient> clients = stagingClientRepo.findAll();
        for (StagingClient client : clients) {
            String pharmacyId = client.getPharmacyId();
            String sourceAffiliationNum = client.getSourceAffiliationNum();
            if (dimClientRepo.findBySourceAffiliationNumAndPharmacyId(sourceAffiliationNum, pharmacyId).isEmpty()) {
                DimClient dim = new DimClient();
                dim.setPharmacyId(pharmacyId);
                dim.setSourceAffiliationNum(sourceAffiliationNum);
                dim.setClientName(client.getClientName());
                dim.setClientType(client.getClientType());
                dim.setEmail(client.getEmail());
                dim.setPhone(client.getPhone());
                dim.setSourceLastUpdated(client.getSourceLastUpdated());
                dimClientRepo.save(dim);
            }
        }

    }

    public void syncFacts() {
        if (syncLogRepo.findById(1L).isEmpty() && factSaleRepo.count() > 0) {
            Long lastStagingId = stagingSaleLineRepo.findTopByOrderByIdDesc()
                    .map(s -> s.getId()).orElse(-1L);
            Long lastFactId = factSaleRepo.findTopByOrderBySourceSaleLineIdDesc()
                    .map(f -> f.getSourceSaleLineId()).orElse(-2L);
            if (lastStagingId.equals(lastFactId)) {
                SyncLog log = new SyncLog();
                log.setId(1L);
                log.setLastSyncedAt(LocalDateTime.now(java.time.ZoneId.of("Africa/Kigali")));
                syncLogRepo.save(log);
                return;
            }
        }

        LocalDateTime lastSync = syncLogRepo.findById(1L)
                .map(s -> s.getLastSyncedAt())
                .orElse(LocalDateTime.of(2020, 1, 1, 0, 0));
        List<StagingSaleLine> lines = stagingSaleLineRepo.findBySyncedAtAfter(lastSync);

        for (StagingSaleLine line : lines) {
                if (factSaleRepo.existsBySourceSaleLineId(line.getId())) continue;

            // fetch the parent sale (we need date, invoiceId, numClient from it) , if there
            // is no parent sale, skip , search the next id
            Optional<StagingSale> saleOpt = stagingSaleRepo.findById(line.getSaleId());
            if (saleOpt.isEmpty())
                continue;
            StagingSale sale = saleOpt.get();

            // parse the date from invoiceTime string (take first 10 chars:
            // "yyyy-MM-dd")
            LocalDate date = LocalDate.parse(sale.getInvoiceTime().substring(0, 10));

            // find or create DimDate row for this date → we need its id
            Optional<DimDate> existingDate = dimDateRepo.findByFullDate(date);

            DimDate dimDate;
            if (existingDate.isPresent()) {
                dimDate = existingDate.get(); // already exists → use it
            } else {
                dimDate = dimDateRepo.save(new DimDate(date)); // doesn't exist → create and save it
            }

            // find the DimProduct and get its id (nullable — product may be
            // missing)
            Long productId = dimProductRepo
                    .findBySourceProductIdAndPharmacyId(line.getSourceProductId(), line.getPharmacyId())
                    .map(p -> p.getId())
                    .orElse(null);

            // Step 7: find the DimClient and get its id (nullable — client may be missing)
            Long clientId = dimClientRepo
                    .findBySourceAffiliationNumAndPharmacyId(sale.getNumClient(), line.getPharmacyId())
                    .map(c -> c.getId()) // map the DimClient and saved in client id variable
                    .orElse(null); // if there is no client, set clientId to null

            // Step 8: build the FactSale row and set all fields
            FactSale fact = new FactSale(); // constructor auto-sets syncedAt = now
            fact.setDateId(dimDate.getId()); // FK to dim_date
            fact.setProductId(productId); // FK to dim_product already extracted above line 156 (nullable)
            fact.setClientId(clientId); // FK to dim_client already extracted above line 159 (nullable)
            fact.setPharmacyId(line.getPharmacyId());
            fact.setSourceSaleLineId(line.getId());
            fact.setSourceInvoiceId(sale.getSourceInvoiceId());
            fact.setQuantity(line.getQuantity());
            fact.setUnitPrice(line.getUnitPrice());
            fact.setCostPrice(line.getCostPrice());
            fact.setTva(line.getTva());
            fact.setInsurance(sale.getNumClient());

            // We check both are not null first because if either is null, multiplying them
            // would crash.
            // If both exist → calculate. If either is missing → store null.
            fact.setTotalAmount(line.getQuantity() != null && line.getUnitPrice() != null
                    ? line.getQuantity() * line.getUnitPrice()
                    : null);

            factSaleRepo.save(fact);

            SyncLog log = syncLogRepo.findById(1L).orElse(new SyncLog());
            log.setId(1L);
            log.setLastSyncedAt(LocalDateTime.now(java.time.ZoneId.of("Africa/Kigali")));
            syncLogRepo.save(log);
        }

    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        sync();
    }

    public boolean isSyncing() {
        return syncing;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void sync() {
        syncing = true;
        try {
            syncPharmacies();
            syncProducts();
            syncClients();
            syncFacts();
        } finally {
            syncing = false;
        }
    }

}
