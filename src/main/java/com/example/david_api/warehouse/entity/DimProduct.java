package com.example.david_api.warehouse.entity;

import jakarta.persistence.*;


@Entity
@Table(name = "dim_product")
public class DimProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer sourceProductId;

    @Column(nullable = false)
    private String pharmacyId;

    private String productName;
    private String productCode;
    private String barcode;
    private Double unitPrice;
    private Double costPrice;
    private Double tvaRate;
    private String family;
private String sourceLastUpdated;

    public DimProduct() {}

    public Long getId() { return id; }

    public Integer getSourceProductId() { return sourceProductId; }
    public void setSourceProductId(Integer sourceProductId) { this.sourceProductId = sourceProductId; }

    public String getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(String pharmacyId) { this.pharmacyId = pharmacyId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Double getCostPrice() { return costPrice; }
    public void setCostPrice(Double costPrice) { this.costPrice = costPrice; }

    public Double getTvaRate() { return tvaRate; }
    public void setTvaRate(Double tvaRate) { this.tvaRate = tvaRate; }

    public String getFamily() { return family; }
    public void setFamily(String family) { this.family = family; }

    public String getSourceLastUpdated() { return sourceLastUpdated; }
    public void setSourceLastUpdated(String sourceLastUpdated) { this.sourceLastUpdated = sourceLastUpdated; }
}