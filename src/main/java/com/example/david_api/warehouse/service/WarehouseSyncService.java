package com.example.david_api.warehouse.service;

import org.springframework.scheduling.annotation.Async;
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
import java.util.concurrent.atomic.AtomicBoolean;

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

    // AtomicBoolean, not a plain boolean: sync() can now be triggered from
    // several places at once (nightly cron, app-startup, and — once wired up
    // in IngestionController — right after an ingest POST) instead of just
    // the near-never-overlapping cron/startup pair it had before. A plain
    // boolean's check-then-set in sync() isn't atomic, so two triggers
    // landing close together could both see "not syncing" and both proceed,
    // racing to insert the same rows. compareAndSet makes "check and claim"
    // one step, so only one sync ever actually runs at a time.
    private final AtomicBoolean syncing = new AtomicBoolean(false);

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

    public void syncPharmacies(LocalDateTime lastSync) {
        List<StagingProduct> products = stagingProductRepo.findBySyncedAtAfter(lastSync);
        for (StagingProduct product : products) {
            String pharmacyId = product.getPharmacyId();
            if (dimPharmacyRepo.findByPharmacyId(pharmacyId).isEmpty()) {
                DimPharmacy dim = new DimPharmacy();
                dim.setPharmacyId(pharmacyId);
                dimPharmacyRepo.save(dim);
            }
        }
    }

    public void syncProducts(LocalDateTime lastSync) {
        List<StagingProduct> products = stagingProductRepo.findBySyncedAtAfter(lastSync);
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

    public void syncClients(LocalDateTime lastSync) {
        List<StagingClient> clients = stagingClientRepo.findBySyncedAtAfter(lastSync);
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

    // lastSync is now read once in sync() and shared across all four steps
    // (see sync() below) instead of each method reading/writing its own
    // watermark. This bootstrap check stays scoped to facts only: it's for
    // the one-time case where fact_sale is already fully populated (e.g. a
    // migration) before sync_log has ever been written, so the very first
    // run doesn't replay the whole history. It no longer saves the log
    // itself — sync() does that once, after all four steps finish — it
    // just skips the (redundant) per-row loop below.
    public void syncFacts(LocalDateTime lastSync) {
        if (syncLogRepo.findById(1L).isEmpty() && factSaleRepo.count() > 0) {
            Long lastStagingId = stagingSaleLineRepo.findTopByOrderByIdDesc()
                    .map(s -> s.getId()).orElse(-1L);
            Long lastFactId = factSaleRepo.findTopByOrderBySourceSaleLineIdDesc()
                    .map(f -> f.getSourceSaleLineId()).orElse(-2L);
            if (lastStagingId.equals(lastFactId)) {
                return;
            }
        }

    // Fetch only sale lines that arrived after the last sync bookmark.
        List<StagingSaleLine> lines = stagingSaleLineRepo.findBySyncedAtAfter(lastSync);

        // For each StagingSaleLine, if it exist in fact_sale, pick the next one (this is the dedup guard), 
        // if it doesnt  exist close the loop and keep going.
        for (StagingSaleLine line : lines) {
                if (factSaleRepo.existsBySourceSaleLineId(line.getId())) continue;

            // fetch the parent sale (we need date, invoiceId, numClient from it) , if there
            // is no parent sale, skip , search the next id
            //if the sale exists sale, we can extract the date and client information from it. put sale.opt in sale
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
            fact.setSourceInvoiceId(sale.getSourceInvoiceId());// from parent sale
            fact.setInvoiceTime(sale.getInvoiceTime());// from parent sale
            fact.setQuantity(line.getQuantity());
            fact.setUnitPrice(line.getUnitPrice());
            fact.setCostPrice(line.getCostPrice());
            fact.setTva(line.getTva());
            fact.setInsurance(sale.getNumClient());// from parent sale

            // We check both are not null first because if either is null, multiplying them
            // would crash.
            // If both exist → calculate. If either is missing → store null.
            fact.setTotalAmount(line.getQuantity() != null && line.getUnitPrice() != null
                    ? line.getQuantity() * line.getUnitPrice()
                    : null);

            factSaleRepo.save(fact);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        sync();
    }

    public boolean isSyncing() {
        return syncing.get();
    }

    // Fire a sync from a request thread (e.g. right after an ingest POST)
    // without making the caller wait for it. sync()'s own compareAndSet
    // guard means this is safe to call often — if a sync is already running
    // (from the cron, startup, or another ingest that just landed), this
    // call is a no-op and the in-flight run picks up the new rows instead.
    @Async
    public void triggerSyncAsync() {
        sync();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void sync() {
        if (!syncing.compareAndSet(false, true)) return;
        try {
            // Read the watermark once and share it across all four steps —
            // previously only syncFacts read/wrote sync_log, so the other
            // three had no way to skip rows they'd already processed and
            // fell back to scanning the entire staging table (16k+ products,
            // 3k+ clients) every single run. Capture the cutoff BEFORE
            // reading, same reasoning as before: the watermark may only ever
            // advance to this snapshot, never to "now" — anything ingested
            // while this run is still in flight keeps a syncedAt after the
            // snapshot, so the next run (cron, startup, or the next ingest
            // trigger) picks it up instead of it being skipped forever.
            LocalDateTime lastSync = syncLogRepo.findById(1L)
                    .map(SyncLog::getLastSyncedAt)
                    .orElse(LocalDateTime.of(2020, 1, 1, 0, 0));
            LocalDateTime snapshotTime = LocalDateTime.now(java.time.ZoneId.of("Africa/Kigali"));

            syncPharmacies(lastSync);
            syncProducts(lastSync);
            syncClients(lastSync);
            syncFacts(lastSync);

            // Only advance the watermark after every step above has
            // finished — if one throws, sync_log is left untouched so the
            // next run retries the whole window instead of skipping it.
            SyncLog log = syncLogRepo.findById(1L).orElse(new SyncLog());
            log.setId(1L);
            log.setLastSyncedAt(snapshotTime);
            syncLogRepo.save(log);
        } finally {
            syncing.set(false);
        }
    }

}
