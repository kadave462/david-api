package com.example.david_api.ingestion.repository;

import com.example.david_api.ingestion.entity.StagingProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StagingProductRepository extends JpaRepository<StagingProduct, Long> {

    boolean existsBySourceProductIdAndPharmacyId(Integer sourceProductId, String pharmacyId);
    List<StagingProduct> findBySyncedAtAfter(LocalDateTime syncedAt);

}
