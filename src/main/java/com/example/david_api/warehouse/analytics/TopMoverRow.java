package com.example.david_api.warehouse.analytics;

public interface TopMoverRow {
    // the pharmacy POS's own product ID (dim_product.source_product_id) —
    // the same number used as staging_stock.item_id for the LEFT JOIN below.
    // Exposed here so the frontend can show it directly instead of someone
    // having to grep the raw /ingest/products feed to find it.
    Integer getItemId();
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
    String getExpirationDate(); // from the same currently-open lot as the fields above; null if no matching lot

    // Most recent sale of this product within the requested date range
    // (MAX(fs.invoice_time)) — a plain aggregate, not tied to any lot.
    String getLastSale();
}
