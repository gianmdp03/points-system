package com.tech.point_system.service;

import com.tech.point_system.dto.sale.SaleDetailDTO;
import com.tech.point_system.dto.sale.SaleRequestDTO;

public interface SaleService {
    SaleDetailDTO addSale(SaleRequestDTO dto);
    SaleDetailDTO updateSale(Long id, SaleRequestDTO dto);
    SaleDetailDTO getSaleById(Long id);
    void deleteSale(Long id);
}
