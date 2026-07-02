package com.example.david_api.ingestion.dto;

public class ClientDTO {
    private String sourceAffiliationNum;
    private String clientName;
    private String clientType;
    private String email;
    private String phone;
    private String sourceLastUpdated;

    public String getSourceAffiliationNum() { return sourceAffiliationNum; }
    public void setSourceAffiliationNum(String sourceAffiliationNum) { this.sourceAffiliationNum = sourceAffiliationNum; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSourceLastUpdated() { return sourceLastUpdated; }
    public void setSourceLastUpdated(String sourceLastUpdated) { this.sourceLastUpdated = sourceLastUpdated; }
}
