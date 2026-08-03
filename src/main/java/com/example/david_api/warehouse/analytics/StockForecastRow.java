package com.example.david_api.warehouse.analytics;

public interface StockForecastRow {

    Integer getItemId();
    String getItemName();
    Long getCurrentStock();
    Double getAvgDailySales();
    Double getDaysRemaining();

}
