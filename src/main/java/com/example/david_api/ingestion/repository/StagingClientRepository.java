package com.example.david_api.ingestion.repository;

import com.example.david_api.ingestion.entity.StagingClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StagingClientRepository extends JpaRepository<StagingClient, Long> {

    boolean existsBySourceAffiliationNumAndPharmacyId(String sourceAffiliationNum, String pharmacyId);
    List<StagingClient> findBySyncedAtAfter(LocalDateTime syncedAt);

}
