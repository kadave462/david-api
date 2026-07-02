package com.example.david_api.ingestion.dto;

public class SaleLineDTO {
    private Integer sourceProductId;
    private String productCode;
    private Integer quantity;
    private Double unitPrice;
    private Double costPrice;
    private Double tva;

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
}
