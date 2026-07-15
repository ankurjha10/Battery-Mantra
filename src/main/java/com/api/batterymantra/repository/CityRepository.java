package com.api.batterymantra.repository;

import com.api.batterymantra.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CityRepository extends JpaRepository<City, UUID> {
    boolean existsByCityNameIgnoreCase(String cityName);
}
