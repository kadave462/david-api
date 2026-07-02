package com.example.david_api.ingestion.dto;

import java.util.List;

public class SaleDTO {
    private Integer sourceInvoiceId;
    private String numClient;
    private Double total;
    private Double tva;
    private Double reduction;
    private String invoiceTime;
    private String employee;
    private String payType;
    private List<SaleLineDTO> lines;

    public Integer getSourceInvoiceId() { return sourceInvoiceId; }
    public void setSourceInvoiceId(Integer sourceInvoiceId) { this.sourceInvoiceId = sourceInvoiceId; }

    public String getNumClient() { return numClient; }
    public void setNumClient(String numClient) { this.numClient = numClient; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Double getTva() { return tva; }
    public void setTva(Double tva) { this.tva = tva; }

    public Double getReduction() { return reduction; }
    public void setReduction(Double reduction) { this.reduction = reduction; }

    public String getInvoiceTime() { return invoiceTime; }
    public void setInvoiceTime(String invoiceTime) { this.invoiceTime = invoiceTime; }

    public String getEmployee() { return employee; }
    public void setEmployee(String employee) { this.employee = employee; }

    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }

    public List<SaleLineDTO> getLines() { return lines; }
    public void setLines(List<SaleLineDTO> lines) { this.lines = lines; }
}
