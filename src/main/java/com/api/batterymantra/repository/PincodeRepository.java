package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Pincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PincodeRepository extends JpaRepository<Pincode, UUID> {
    List<Pincode> findByCity_CityId(UUID cityId);
    boolean existsByCode(String code);
}
