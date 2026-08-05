package com.example.david_api.warehouse.analytics;

public interface TopMoverRow {
    String getProductName();
    Long getTotalQuantity();
    Double getTotalRevenue();
    Double getCost();
    Double getProfit();

    // Current stock, summed across ALL of a product's open lots (from
    // staging_stock, NOT fact_sale) — nullable, since a product with sales
    // history might have no matching stock record. Long, not Integer:
    // SUM() over an int column in Postgres returns bigint, and getTotalQuantity()
    // above is Long for the exact same reason (SUM(fs.quantity)).
    Long getInitialQuantity();
    String getBatchNumber();   // the actual lot/batch identifier
    String getIdLot();         // despite the name, this column holds a documented date, not an id
    Long getLiveQuantity();
}
