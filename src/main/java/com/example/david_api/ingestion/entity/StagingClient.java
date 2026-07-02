package com.example.david_api.ingestion.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staging_clients")
public class StagingClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pharmacyId;
    private String sourceAffiliationNum;
    private String clientName;
    private String clientType;
    private String email;
    private String phone;
    private String sourceLastUpdated;
    private LocalDateTime syncedAt;

    public StagingClient() {
        this.syncedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public String getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(String pharmacyId) { this.pharmacyId = pharmacyId; }

    public String getSourceAffiliationNum() { return sourceAffiliationNum; }
    public void setSourceAffiliationNum(String sourceAffiliationNum) { this.sourceAffiliationNum = sourceAffiliationNum; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSourceLastUpdated() { return sourceLastUpdated; }
    public void setSourceLastUpdated(String sourceLastUpdated) { this.sourceLastUpdated = sourceLastUpdated; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }
}
