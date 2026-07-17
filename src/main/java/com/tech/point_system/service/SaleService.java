package com.tech.point_system.service;

import com.tech.point_system.dto.sale.SaleDetailDTO;
import com.tech.point_system.dto.sale.SaleListDTO;
import com.tech.point_system.dto.sale.SaleRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SaleService {
    SaleDetailDTO addSale(String companyAdminId, SaleRequestDTO dto);
    Page<SaleListDTO> listCompaniesSales(String companyAdminId, Long companyId, Pageable pageable);
    SaleDetailDTO getSaleById(String companyAdminId, Long companyId, Long id);
}
