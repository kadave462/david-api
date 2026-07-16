package com.example.david_api.warehouse.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "dim_pharmacy")
public class DimPharmacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String pharmacyId;
    private String pharmacyName;
    private String location;
    
    public DimPharmacy() {}

    public Long getId() { return id; }

    public String getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(String pharmacyId) { this.pharmacyId = pharmacyId; }

    public String getPharmacyName() { return pharmacyName; }
    public void setPharmacyName(String pharmacyName) { this.pharmacyName = pharmacyName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
