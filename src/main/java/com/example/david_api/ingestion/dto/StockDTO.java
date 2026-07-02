package com.example.david_api.ingestion.dto;

public class StockDTO {
    private String hmac;
    private Integer stockLocationId;
    private Integer idProduct;
    private String nameProduct;
    private String idLotS;
    private String dateExp;
    private Integer qtyStart;
    private Integer qtyLive;
    private String bonLivraison;
    private String numClient;
    private String idLot;
    private Double revient;
    private String code;
    private String employee;
    private Double prix;

    public String getHmac() { return hmac; }
    public void setHmac(String hmac) { this.hmac = hmac; }

    public Integer getStockLocationId() { return stockLocationId; }
    public void setStockLocationId(Integer stockLocationId) { this.stockLocationId = stockLocationId; }

    public Integer getIdProduct() { return idProduct; }
    public void setIdProduct(Integer idProduct) { this.idProduct = idProduct; }

    public String getNameProduct() { return nameProduct; }
    public void setNameProduct(String nameProduct) { this.nameProduct = nameProduct; }

    public String getIdLotS() { return idLotS; }
    public void setIdLotS(String idLotS) { this.idLotS = idLotS; }

    public String getDateExp() { return dateExp; }
    public void setDateExp(String dateExp) { this.dateExp = dateExp; }

    public Integer getQtyStart() { return qtyStart; }
    public void setQtyStart(Integer qtyStart) { this.qtyStart = qtyStart; }

    public Integer getQtyLive() { return qtyLive; }
    public void setQtyLive(Integer qtyLive) { this.qtyLive = qtyLive; }

    public String getBonLivraison() { return bonLivraison; }
    public void setBonLivraison(String bonLivraison) { this.bonLivraison = bonLivraison; }

    public String getNumClient() { return numClient; }
    public void setNumClient(String numClient) { this.numClient = numClient; }

    public String getIdLot() { return idLot; }
    public void setIdLot(String idLot) { this.idLot = idLot; }

    public Double getRevient() { return revient; }
    public void setRevient(Double revient) { this.revient = revient; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getEmployee() { return employee; }
    public void setEmployee(String employee) { this.employee = employee; }

    public Double getPrix() { return prix; }
    public void setPrix(Double prix) { this.prix = prix; }
}
