package com.example.david_api.ingestion.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "staging_sales")
public class StagingSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pharmacyId;
    private Integer sourceInvoiceId;
    private String numClient;
    private Double total;
    private Double tva;
    private Double reduction;
    private String invoiceTime;
    private String employee;
    private String payType;
    private LocalDateTime syncedAt;

    public StagingSale() {
        this.syncedAt = LocalDateTime.now(ZoneId.of("Africa/Kigali"));
    }

    public Long getId() { return id; }

    public String getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(String pharmacyId) { this.pharmacyId = pharmacyId; }

    public Integer getSourceInvoiceId() { return sourceInvoiceId; }
    public void setSourceInvoiceId(Integer sourceInvoiceId) { this.sourceInvoiceId = sourceInvoiceId; }

    public String getNumClient() { return numClient; }
    public void setNumClient(String numClient) { this.numClient = numClient; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Double getTva() { return tva; }
    public void setTva(Double tva) { this.tva = tva; }

    public Double getReduction() { return reduction; }
    public void setReduction(Double reduction) { this.reduction = reduction; }

    public String getInvoiceTime() { return invoiceTime; }
    public void setInvoiceTime(String invoiceTime) { this.invoiceTime = invoiceTime; }

    public String getEmployee() { return employee; }
    public void setEmployee(String employee) { this.employee = employee; }

    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }
}
