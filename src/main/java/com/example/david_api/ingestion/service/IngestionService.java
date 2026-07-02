package com.example.david_api.ingestion.service;

import com.example.david_api.ingestion.dto.ClientDTO;
import com.example.david_api.ingestion.dto.ProductDTO;
import com.example.david_api.ingestion.dto.SaleDTO;
import com.example.david_api.ingestion.dto.SaleLineDTO;
import com.example.david_api.ingestion.dto.StockDTO;
import com.example.david_api.ingestion.entity.StagingClient;
import com.example.david_api.ingestion.entity.StagingProduct;
import com.example.david_api.ingestion.entity.StagingSale;
import com.example.david_api.ingestion.entity.StagingSaleLine;
import com.example.david_api.ingestion.entity.StagingStock;
import com.example.david_api.ingestion.repository.StagingClientRepository;
import com.example.david_api.ingestion.repository.StagingProductRepository;
import com.example.david_api.ingestion.repository.StagingSaleLineRepository;
import com.example.david_api.ingestion.repository.StagingSaleRepository;
import com.example.david_api.ingestion.repository.StagingStockRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class IngestionService {
    private final StagingProductRepository productRepository;
    private final StagingClientRepository clientRepository;
    private final StagingSaleRepository saleRepository;
    private final StagingSaleLineRepository saleLineRepository;
    private final StagingStockRepository stockRepository;

    public IngestionService(StagingProductRepository productRepository,
                            StagingClientRepository clientRepository,
                            StagingSaleRepository saleRepository,
                            StagingSaleLineRepository saleLineRepository,
                            StagingStockRepository stockRepository) {
        this.productRepository = productRepository;
        this.clientRepository = clientRepository;
        this.saleRepository = saleRepository;
        this.saleLineRepository = saleLineRepository;
        this.stockRepository = stockRepository;
    }

    public List<StagingProduct> getProducts() {
        return productRepository.findAll();
    }

    public List<StagingClient> getClients() {
        return clientRepository.findAll();
    }

    public List<StagingSale> getSales() {
        return saleRepository.findAll();
    }

    public void saveSales(List<SaleDTO> sales, String pharmacyId, String apiKey) {
        if (!apiKey.equals("TRAMEDNYARUGENGE-2026")) {
            throw new RuntimeException("Unauthorized: invalid API key");
        }
        for (SaleDTO dto : sales) {
            StagingSale sale = new StagingSale();
            sale.setPharmacyId(pharmacyId);
            sale.setSourceInvoiceId(dto.getSourceInvoiceId());
            sale.setNumClient(dto.getNumClient());
            sale.setTotal(dto.getTotal());
            sale.setTva(dto.getTva());
            sale.setReduction(dto.getReduction());
            sale.setInvoiceTime(dto.getInvoiceTime());
            sale.setEmployee(dto.getEmployee());
            sale.setPayType(dto.getPayType());
            StagingSale savedSale = saleRepository.save(sale);

            if (dto.getLines() != null) {
                for (SaleLineDTO lineDto : dto.getLines()) {
                    StagingSaleLine line = new StagingSaleLine();
                    line.setSaleId(savedSale.getId());
                    line.setPharmacyId(pharmacyId);
                    line.setSourceProductId(lineDto.getSourceProductId());
                    line.setProductCode(lineDto.getProductCode());
                    line.setQuantity(lineDto.getQuantity());
                    line.setUnitPrice(lineDto.getUnitPrice());
                    line.setCostPrice(lineDto.getCostPrice());
                    line.setTva(lineDto.getTva());
                    saleLineRepository.save(line);
                }
            }
        }
    }

    public void saveClients(List<ClientDTO> clients, String pharmacyId, String apiKey) {
        if (!apiKey.equals("TRAMEDNYARUGENGE-2026")) {
            throw new RuntimeException("Unauthorized: invalid API key");
        }
        for (ClientDTO dto : clients) {
            StagingClient entity = new StagingClient();
            entity.setPharmacyId(pharmacyId);
            entity.setSourceAffiliationNum(dto.getSourceAffiliationNum());
            entity.setClientName(dto.getClientName());
            entity.setClientType(dto.getClientType());
            entity.setEmail(dto.getEmail());
            entity.setPhone(dto.getPhone());
            entity.setSourceLastUpdated(dto.getSourceLastUpdated());
            clientRepository.save(entity);
        }
    }

    public List<StagingStock> getStock() {
        return stockRepository.findAll();
    }

    public void saveStock(List<StockDTO> stockList, String pharmacyId, String apiKey) {
        if (!apiKey.equals("TRAMEDNYARUGENGE-2026")) {
            throw new RuntimeException("Unauthorized: invalid API key");
        }
        for (StockDTO dto : stockList) {
            StagingStock entity = new StagingStock();
            entity.setPharmacyId(pharmacyId);
            entity.setHmac(dto.getHmac());
            entity.setStockLocationId(dto.getStockLocationId());
            entity.setItemId(dto.getItemId());
            entity.setItemName(dto.getItemName());
            entity.setBatchNumber(dto.getBatchNumber());
            entity.setExpirationDate(dto.getExpirationDate());
            entity.setInitialQuantity(dto.getInitialQuantity());
            entity.setQuantity(dto.getQuantity());
            entity.setUnitCost(dto.getUnitCost());
            entity.setUnitPrice(dto.getUnitPrice());
            entity.setSupplierName(dto.getSupplierName());
            entity.setReference(dto.getReference());
            entity.setItemCode(dto.getItemCode());
            entity.setEmployee(dto.getEmployee());
            entity.setIdLot(dto.getIdLot());
            stockRepository.save(entity);
        }
    }

    public void saveProducts(List<ProductDTO> products, String pharmacyId, String apiKey) {
            if (!apiKey.equals("TRAMEDNYARUGENGE-2026")) {
        throw new RuntimeException("Unauthorized: invalid API key");
    }
        for (ProductDTO dto : products) {
            StagingProduct entity = new StagingProduct();
            entity.setPharmacyId(pharmacyId);
            entity.setSourceProductId(dto.getSourceProductId());
            entity.setProductName(dto.getProductName());
            entity.setProductCode(dto.getProductCode());
            entity.setBarcode(dto.getBarcode());
            entity.setUnitPrice(dto.getUnitPrice());
            entity.setCostPrice(dto.getCostPrice());
            entity.setTvaRate(dto.getTvaRate());
            entity.setFamily(dto.getFamily());
            entity.setSourceLastUpdated(dto.getSourceLastUpdated());
            productRepository.save(entity);
        }
    }

}
