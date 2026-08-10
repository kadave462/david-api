package com.example.david_api.ingestion.repository;

import com.example.david_api.ingestion.entity.StagingStock;
import com.example.david_api.warehouse.analytics.ExpiringStockRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Repository
public interface StagingStockRepository extends JpaRepository<StagingStock, Long> {
    Optional<StagingStock> findTopByOrderBySyncedAtDesc();

    // The natural key for "this physical lot" — same triple every other
    // query in this codebase already dedupes on (item_id, batch_number,
    // id_lot; see FactSaleRepository/expiringStock above). Used by
    // saveStock() to find whether a row for this lot already exists so it
    // can be updated in place instead of appending a new one every sync.
    Optional<StagingStock> findByPharmacyIdAndItemIdAndBatchNumberAndIdLot(
            String pharmacyId, Integer itemId, String batchNumber, String idLot);

    // Lots expiring between :after and :before (controller defaults these to
    // today and today+30 days), soonest-expiring first. Deliberately NOT
    // grouped/summed by product like the other analytics queries —
    // expiration is a per-LOT property, so a product with 2 lots (one
    // expiring soon, one not) needs to show up as 2 separate rows, not one
    // blended number.
    //
    // Two steps, because DISTINCT ON forces its ORDER BY to start with the
    // DISTINCT ON columns — which would sort the FINAL output by
    // item_id/batch_number, not by soonest-expiring. So latest_per_lot picks
    // each lot's latest sync first (order doesn't matter beyond that), and
    // the outer SELECT does the real filtering/sorting afterward.
    //
    // expiration_date is stored as text (not a real date column), so it's
    // cast with ::date to compare and sort it properly — this assumes the
    // stored format is a normal parseable date string (e.g. YYYY-MM-DD);
    // if the source system uses something Postgres can't parse, this cast
    // will error rather than silently sort wrong.
    //
    // expiration_date::date >= '2020-01-01' excludes garbage data confirmed
    // in production: hundreds of lots carry a placeholder date somewhere in
    // 2001 (looks like the source POS system's "no real date entered"
    // default, not an actual expiration 25 years ago). Now that :after has
    // its own explicit lower bound too, this is mostly a defensive backstop
    // rather than the only thing doing the filtering.
    //
    // quantity > 0 excludes lots that are already fully sold/used up. Needed
    // now that SparkBind syncs QTY_LIVE >= 0 (previously > 0) — sold-out
    // lots that used to just vanish from the feed now show up with their
    // real, frozen-at-zero quantity. Still relevant to know they expired,
    // but not as "stock you need to move," so this panel excludes them
    // rather than mixing genuinely actionable stock with dead lots.
    @Query(value = """
                    WITH latest_per_lot AS (
                        SELECT DISTINCT ON (item_id, batch_number, id_lot)
                               item_name, batch_number, expiration_date, initial_quantity, quantity, unit_cost
                        FROM staging_stock
                        ORDER BY item_id, batch_number, id_lot, synced_at DESC
                    )
                    SELECT item_name, batch_number, expiration_date, initial_quantity, quantity, unit_cost
                    FROM latest_per_lot
                    WHERE expiration_date IS NOT NULL
                      AND expiration_date::date >= '2020-01-01'
                      AND expiration_date::date BETWEEN :after AND :before
                      AND quantity > 0
                    ORDER BY expiration_date::date ASC
                    """, nativeQuery = true)
    List<ExpiringStockRow> expiringStock(@Param("after") LocalDate after, @Param("before") LocalDate before);
}
