package com.tech.point_system.service.impl;

import com.tech.point_system.dto.sale.SaleDetailDTO;
import com.tech.point_system.dto.sale.SaleListDTO;
import com.tech.point_system.dto.sale.SaleRequestDTO;
import com.tech.point_system.event.SaleCreatedEvent;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.SaleMapper;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.Sale;
import com.tech.point_system.repository.ClientRepository;
import com.tech.point_system.repository.SaleRepository;
import com.tech.point_system.service.CompanyAccessValidator;
import com.tech.point_system.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {
  private final SaleRepository repository;
  private final SaleMapper mapper;
  private final ClientRepository clientRepository;
  private final CompanyAccessValidator companyAccessValidator;
  private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public SaleDetailDTO addSale(String companyAdminId, SaleRequestDTO dto) {
        Company company = companyAccessValidator.validateAccess(dto.companyId(), companyAdminId);

        Client client = clientRepository.findByDniAndCountry(dto.dni(), dto.country())
                .orElseThrow(()-> new NotFoundException("Client not found"));

        Sale sale = mapper.toEntity(dto);
        sale.setCompany(company);
        sale.setClient(client);
        sale.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        sale = repository.save(sale);

        applicationEventPublisher.publishEvent(new SaleCreatedEvent(dto.amount(), company, client));
        return mapper.toDetailDTO(sale);
    }

  @Override
  public Page<SaleListDTO> listCompaniesSales(String companyAdminId, Long companyId, Pageable pageable){
      companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
      return repository.findByCompanyId(companyId, pageable).map(mapper::toListDTO);
  }

  @Override
  public SaleDetailDTO getSaleById(String companyAdminId, Long companyId, Long id) {
      companyAccessValidator.checkAccessOnly(companyId, companyAdminId);
      Sale sale = repository.findById(id).orElseThrow(() -> new NotFoundException("Sale ID not found!"));
      if (sale.getCompany() == null || !sale.getCompany().getId().equals(companyId)) {
          throw new NotFoundException("Sale ID not found for company " + companyId);
      }
      return mapper.toDetailDTO(sale);
  }
}
