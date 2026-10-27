package com.api.batterymantra.repository;

import com.api.batterymantra.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserUserId(UUID userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteAllByUserUserId(UUID userId);

    List<Address> findAllByUserUserIdAndIsDeletedFalse(UUID userId);

    Optional<Address> findByUserUserIdAndIsDefaultTrueAndIsDeletedFalse(UUID userId);
}
