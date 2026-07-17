package com.tech.point_system.repository;

import com.tech.point_system.model.PointsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {}
