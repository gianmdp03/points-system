package com.tech.point_system.mapper;

import com.tech.point_system.dto.pointsTransaction.PointsTransactionDetailDTO;
import com.tech.point_system.model.PointsTransaction;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface PointsTransactionMapper {
    PointsTransactionDetailDTO toDetailDTO(PointsTransaction entity);
}
