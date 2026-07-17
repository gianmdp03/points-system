package com.tech.point_system.service.impl;

import com.tech.point_system.dto.sale.SaleDetailDTO;
import com.tech.point_system.dto.sale.SaleListDTO;
import com.tech.point_system.dto.sale.SaleRequestDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.SaleMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.Sale;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.SaleRepository;
import com.tech.point_system.security.user.repository.UserRepository;
import com.tech.point_system.security.user.service.CompanyAccessValidator;
import com.tech.point_system.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {
  private final SaleRepository repository;
  private final SaleMapper mapper;
  private final CompanyRepository companyRepository;
  private final UserRepository userRepository;
  private final CompanyAccessValidator companyAccessValidator;

  @Override
  @Transactional
  public SaleDetailDTO addSale(String companyAdminId, SaleRequestDTO dto) {
    Company company = companyAccessValidator.validateAccess(dto.companyId(), companyAdminId);
      Sale sale = mapper.toEntity(dto);
      sale.setCompany(company);
      sale = repository.save(sale);
      return mapper.toDetailDTO(sale);
  }

  @Override
  public Page<SaleListDTO> listCompaniesSales(String companyAdminId, Long companyId, Pageable pageable){
      companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
      Page<Sale> sales = repository.findByCompanyId(companyId, pageable);
      if(sales.isEmpty()){
          return Page.empty();
      }
      return sales.map(mapper::toListDTO);
  }

  @Override
  public SaleDetailDTO getSaleById(String companyAdminId, Long companyId, Long id) {
      companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
    Sale sale = repository.findById(id).orElseThrow(() -> new NotFoundException("Sale ID not found!"));
    return mapper.toDetailDTO(sale);
  }
}
