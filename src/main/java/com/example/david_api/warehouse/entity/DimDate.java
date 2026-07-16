package com.example.david_api.warehouse.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "dim_date")
public class DimDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private LocalDate fullDate;

    private Integer day;
    private Integer month;
    private Integer year;
    private Integer quarter;
    private Integer dayOfWeek;
    private Integer weekOfYear;

    public DimDate() {}

    public DimDate(LocalDate fullDate) {
        this.fullDate   = fullDate;
        this.day        = fullDate.getDayOfMonth();
        this.month      = fullDate.getMonthValue();
        this.year       = fullDate.getYear();
        this.quarter    = (fullDate.getMonthValue() - 1) / 3 + 1;
        this.dayOfWeek  = fullDate.getDayOfWeek().getValue();
        this.weekOfYear = fullDate.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
    }

    public Long getId() { return id; }
    public LocalDate getFullDate() { return fullDate; }
    public Integer getDay() { return day; }
    public Integer getMonth() { return month; }
    public Integer getYear() { return year; }
    public Integer getQuarter() { return quarter; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public Integer getWeekOfYear() { return weekOfYear; }
}
