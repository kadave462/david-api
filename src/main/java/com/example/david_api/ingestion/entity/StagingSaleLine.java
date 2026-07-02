package com.example.david_api.ingestion.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staging_sale_lines")
public class StagingSaleLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long saleId;
    private String pharmacyId;
    private Integer sourceProductId;
    private String productCode;
    private Integer quantity;
    private Double unitPrice;
    private Double costPrice;
    private Double tva;
    private LocalDateTime syncedAt;

    public StagingSaleLine() {
        this.syncedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }

    public String getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(String pharmacyId) { this.pharmacyId = pharmacyId; }

    public Integer getSourceProductId() { return sourceProductId; }
    public void setSourceProductId(Integer sourceProductId) { this.sourceProductId = sourceProductId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Double getCostPrice() { return costPrice; }
    public void setCostPrice(Double costPrice) { this.costPrice = costPrice; }

    public Double getTva() { return tva; }
    public void setTva(Double tva) { this.tva = tva; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }
}
