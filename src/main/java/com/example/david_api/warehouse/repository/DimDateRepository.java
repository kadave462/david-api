package com.example.david_api.warehouse.repository;

import com.example.david_api.warehouse.entity.DimDate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface DimDateRepository extends JpaRepository<DimDate, Long> {
    Optional<DimDate> findByFullDate(LocalDate fullDate);
}
