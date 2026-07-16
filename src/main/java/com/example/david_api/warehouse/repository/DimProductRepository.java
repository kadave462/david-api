package com.example.david_api.warehouse.repository;

import com.example.david_api.warehouse.entity.DimProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DimProductRepository extends JpaRepository<DimProduct, Long> {
    Optional<DimProduct> findBySourceProductIdAndPharmacyId(Integer sourceProductId, String pharmacyId);
}
