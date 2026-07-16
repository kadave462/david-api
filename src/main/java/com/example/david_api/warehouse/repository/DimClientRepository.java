package com.example.david_api.warehouse.repository;

import com.example.david_api.warehouse.entity.DimClient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DimClientRepository extends JpaRepository<DimClient, Long> {
    Optional<DimClient> findBySourceAffiliationNumAndPharmacyId(String sourceAffiliationNum, String pharmacyId);
}
