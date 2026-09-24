package com.repository;

import com.entity.Vehicle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository
        extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByDistributorIdAndActiveTrue(
            Long distributorId
    );

    /*
     * Giữ method cũ cho các chức năng chỉ cần đọc vehicle.
     */
    Optional<Vehicle> findByIdAndDistributorIdAndActiveTrue(
            Long id,
            Long distributorId
    );

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Khóa bản ghi vehicle trong transaction khi kiểm tra sequence.
     *
     * Nếu hai request có cùng sequence đến đồng thời,
     * request thứ hai phải chờ request thứ nhất hoàn thành,
     * sau đó sẽ thấy lastSequence mới và bị từ chối.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT v
            FROM Vehicle v
            WHERE v.id = :vehicleId
              AND v.distributorId = :distributorId
              AND v.active = true
            """)
    Optional<Vehicle> findActiveVehicleForUpdate(
            @Param("vehicleId") Long vehicleId,
            @Param("distributorId") Long distributorId
    );
}