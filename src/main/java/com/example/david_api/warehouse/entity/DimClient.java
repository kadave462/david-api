package com.example.david_api.warehouse.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "dim_client")
public class DimClient {
    @Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@Column(nullable = false)
private String sourceAffiliationNum;
@Column(nullable = false)
private String pharmacyId;

private String clientName;
private String clientType;
private String email;
private String phone;
private String sourceLastUpdated;



public DimClient() {}

public Long getId() { return id; }

public String getSourceAffiliationNum() { return sourceAffiliationNum; }
public void setSourceAffiliationNum(String sourceAffiliationNum) { this.sourceAffiliationNum = sourceAffiliationNum; }

public String getPharmacyId() { return pharmacyId; }
public void setPharmacyId(String pharmacyId) { this.pharmacyId = pharmacyId; }

public String getClientName() { return clientName; }
public void setClientName(String clientName) { this.clientName = clientName; }

public String getClientType() { return clientType; }
public void setClientType(String clientType) { this.clientType = clientType; }

public String getEmail() { return email; }
public void setEmail(String email) { this.email = email; }

public String getPhone() { return phone; }
public void setPhone(String phone) { this.phone = phone; }

public String getSourceLastUpdated() { return sourceLastUpdated; }
public void setSourceLastUpdated(String s) { this.sourceLastUpdated = s; }


    
}
