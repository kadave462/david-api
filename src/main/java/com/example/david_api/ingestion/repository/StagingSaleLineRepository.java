package com.example.david_api.ingestion.repository;

import com.example.david_api.ingestion.entity.StagingSaleLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StagingSaleLineRepository extends JpaRepository<StagingSaleLine, Long> {
    Optional<StagingSaleLine> findTopByOrderBySyncedAtDesc();
}
