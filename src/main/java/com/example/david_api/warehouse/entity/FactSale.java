package com.example.david_api.warehouse.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fact_sale")
public class FactSale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long dateId;

    private Long clientId;
    private Long productId;

    @Column(nullable = false)
    private String pharmacyId;

    @Column(unique = true, nullable = false)
    private Long sourceSaleLineId;

    private Integer sourceInvoiceId;
    private Integer quantity;
    private Double unitPrice;
    private Double costPrice;
    private Double totalAmount;
    private Double tva;
    private LocalDateTime syncedAt;

    public FactSale() {
        this.syncedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Long getDateId() { return dateId; }
    public void setDateId(Long dateId) { this.dateId = dateId; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(String pharmacyId) { this.pharmacyId = pharmacyId; }

    public Long getSourceSaleLineId() { return sourceSaleLineId; }
    public void setSourceSaleLineId(Long sourceSaleLineId) { this.sourceSaleLineId = sourceSaleLineId; }

    public Integer getSourceInvoiceId() { return sourceInvoiceId; }
    public void setSourceInvoiceId(Integer sourceInvoiceId) { this.sourceInvoiceId = sourceInvoiceId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Double getCostPrice() { return costPrice; }
    public void setCostPrice(Double costPrice) { this.costPrice = costPrice; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public Double getTva() { return tva; }
    public void setTva(Double tva) { this.tva = tva; }

    public LocalDateTime getSyncedAt() { return syncedAt; }
}

