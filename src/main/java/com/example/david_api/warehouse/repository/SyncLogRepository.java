package com.example.david_api.warehouse.repository;

import com.example.david_api.warehouse.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
}
