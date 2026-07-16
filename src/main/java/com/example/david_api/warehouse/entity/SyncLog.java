package com.example.david_api.warehouse.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_log")
public class SyncLog {

    @Id
    private Long id;

    private LocalDateTime lastSyncedAt;

    public SyncLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
