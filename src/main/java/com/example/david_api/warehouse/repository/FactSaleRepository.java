package com.example.david_api.warehouse.repository;

import com.example.david_api.warehouse.entity.FactSale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FactSaleRepository extends JpaRepository<FactSale, Long> {
    boolean existsBySourceSaleLineId(Long sourceSaleLineId);
    Optional<FactSale> findTopByOrderBySourceSaleLineIdDesc();
}
