package com.repository;

import com.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByDistributorIdAndActiveTrue(Long distributorId);

    Optional<Vehicle> findByIdAndDistributorIdAndActiveTrue(Long id, Long distributorId);
}