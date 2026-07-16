package com.example.david_api.ingestion.repository;

import com.example.david_api.ingestion.entity.StagingStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface StagingStockRepository extends JpaRepository<StagingStock, Long> {
    boolean existsByHmacAndPharmacyId(String hmac, String pharmacyId);
    Optional<StagingStock> findTopByOrderBySyncedAtDesc();

}
