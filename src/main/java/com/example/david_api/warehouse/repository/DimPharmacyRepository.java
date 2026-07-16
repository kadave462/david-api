package com.example.david_api.warehouse.repository;

import com.example.david_api.warehouse.entity.DimPharmacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DimPharmacyRepository extends JpaRepository<DimPharmacy, Long> {
    Optional<DimPharmacy> findByPharmacyId(String pharmacyId);
}
