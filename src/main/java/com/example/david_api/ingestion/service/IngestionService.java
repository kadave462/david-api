package com.example.david_api.ingestion.service;

import com.example.david_api.ingestion.dto.ProductDTO;
import com.example.david_api.ingestion.entity.StagingProduct;
import com.example.david_api.ingestion.repository.StagingProductRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class IngestionService {
    private final StagingProductRepository productRepository;

    public IngestionService(StagingProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<StagingProduct> getProducts() {
        return productRepository.findAll();
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
