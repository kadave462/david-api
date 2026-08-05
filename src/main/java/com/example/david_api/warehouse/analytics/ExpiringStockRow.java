package com.example.david_api.warehouse.analytics;

// One row per stock LOT (not per product — a product can have several lots,
// each with its own expiration date, so these are never summed together the
// way other analytics rows are).
public interface ExpiringStockRow {
    String getItemName();
    String getBatchNumber();
    String getExpirationDate();
    Integer getInitialQuantity();
    Integer getQuantity();
}
