package com.example.david_api.warehouse.analytics;

public interface TopMoverRow {
    String getProductName();
    Long getTotalQuantity();
    Double getTotalRevenue();
    Double getCost();
    Double getProfit();
}
