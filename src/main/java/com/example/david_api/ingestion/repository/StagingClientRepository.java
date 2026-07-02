package com.example.david_api.ingestion.repository;

import com.example.david_api.ingestion.entity.StagingClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StagingClientRepository extends JpaRepository<StagingClient, Long> {

}
