package com.example.david_api.ingestion.repository;

import com.example.david_api.ingestion.entity.StagingProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StagingProductRepository extends JpaRepository<StagingProduct, Long> {

    boolean existsBySourceProductIdAndPharmacyId(Integer sourceProductId, String pharmacyId);

}
