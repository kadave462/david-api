package com.example.david_api.ingestion.repository;

import com.example.david_api.ingestion.entity.StagingSale;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StagingSaleRepository extends JpaRepository<StagingSale, Long> {

    boolean existsBySourceInvoiceIdAndPharmacyId(Integer sourceInvoiceId, String pharmacyId);
    Optional<StagingSale> findTopByOrderBySyncedAtDesc();


}
