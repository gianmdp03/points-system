package com.tech.point_system.service;

import com.tech.point_system.dto.company.CompanyNameDTO;
import com.tech.point_system.dto.product.ProductListDTO;
import com.tech.point_system.dto.promotion.PromotionListDTO;
import com.tech.point_system.dto.reward.RewardListDTO;
import com.tech.point_system.exception.NotFoundException;
import com.tech.point_system.mapper.ProductMapper;
import com.tech.point_system.mapper.PromotionMapper;
import com.tech.point_system.mapper.RewardMapper;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.Product;
import com.tech.point_system.model.Promotion;
import com.tech.point_system.model.Reward;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.ProductRepository;
import com.tech.point_system.repository.PromotionRepository;
import com.tech.point_system.repository.RewardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicCatalogServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private RewardRepository rewardRepository;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private RewardMapper rewardMapper;
    @Mock
    private PromotionMapper promotionMapper;

    @InjectMocks
    private PublicCatalogService publicCatalogService;

    private Company testCompany;

    @BeforeEach
    void setUp() {
        testCompany = new Company();
        testCompany.setId(10L);
        testCompany.setName("Café Premium");
        testCompany.setIsEnabled(true);
    }

    @Test
    @DisplayName("getPublicCompanyName retorna CompanyNameDTO cuando la empresa existe y está activa")
    void getPublicCompanyName_Success() {
        when(companyRepository.findById(10L)).thenReturn(Optional.of(testCompany));

        CompanyNameDTO result = publicCatalogService.getPublicCompanyName(10L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Café Premium");
    }

    @Test
    @DisplayName("getPublicCompanyName lanza NotFoundException cuando la empresa no existe")
    void getPublicCompanyName_NotFound() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicCatalogService.getPublicCompanyName(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Comercio no encontrado.");
    }

    @Test
    @DisplayName("getPublicCompanyName lanza NotFoundException cuando la empresa está deshabilitada")
    void getPublicCompanyName_DisabledCompany() {
        testCompany.setIsEnabled(false);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(testCompany));

        assertThatThrownBy(() -> publicCatalogService.getPublicCompanyName(10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("El comercio no se encuentra disponible.");
    }

    @Test
    @DisplayName("getPublicPromotions retorna lista mapeada de promociones activas")
    void getPublicPromotions_Success() {
        Promotion promo = new Promotion();
        promo.setId(1L);
        promo.setName("Doble Puntos");
        promo.setMultiplier(BigDecimal.valueOf(2));

        PromotionListDTO promoDTO = new PromotionListDTO(1L, "Doble Puntos", "Promo", null, null, true, BigDecimal.valueOf(2));

        when(promotionRepository.findActivePromotions(eq(10L), any(OffsetDateTime.class))).thenReturn(List.of(promo));
        when(promotionMapper.toListDTO(promo)).thenReturn(promoDTO);

        List<PromotionListDTO> results = publicCatalogService.getPublicPromotions(10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Doble Puntos");
    }

    @Test
    @DisplayName("getPublicProducts retorna lista mapeada de productos")
    void getPublicProducts_Success() {
        Product prod = new Product();
        prod.setId(1L);
        prod.setName("Café Espresso");

        ProductListDTO prodDTO = new ProductListDTO(1L, "Café Espresso", "Delicioso", BigDecimal.valueOf(500), null);

        when(productRepository.findByCompanyId(10L)).thenReturn(List.of(prod));
        when(productMapper.toListDTO(prod)).thenReturn(prodDTO);

        List<ProductListDTO> results = publicCatalogService.getPublicProducts(10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Café Espresso");
    }

    @Test
    @DisplayName("getPublicRewards retorna lista mapeada de premios habilitados")
    void getPublicRewards_Success() {
        Reward reward = new Reward();
        reward.setId(1L);
        reward.setName("Taza de Cerámica");
        reward.setIsEnabled(true);

        RewardListDTO rewardDTO = new RewardListDTO(1L, "Taza de Cerámica", "Taza", 200, true);

        when(rewardRepository.findByCompanyIdAndIsEnabledTrue(10L)).thenReturn(List.of(reward));
        when(rewardMapper.toListDTO(reward)).thenReturn(rewardDTO);

        List<RewardListDTO> results = publicCatalogService.getPublicRewards(10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Taza de Cerámica");
    }
}
