package com.tech.point_system.config;

import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.model.Promotion;
import com.tech.point_system.model.Reward;
import com.tech.point_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PointlyToolsConfig {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final PointsAccountRepository pointsAccountRepository;
    private final RewardRepository rewardRepository;
    private final PromotionRepository promotionRepository;

    public record ClientPointsRequest(String dni, String country, String companyName) {}
    public record ClientPointsResponse(boolean success, String clientName, String companyName, Integer balance, String message) {}
    public record RewardsRequest(String companyName) {}
    public record RewardInfo(String name, String description, Integer costInPoints) {}
    public record RewardsResponse(boolean success, String companyName, List<RewardInfo> rewards, String message) {}
    public record PromotionRequest(String companyName) {}
    public record PromotionResponse(boolean hasActivePromotion, String companyName, String promotionName, String description, Double multiplier) {}

    @Tool(description = "Consulta el saldo actual de puntos acumulados de un cliente en un comercio especifico usando su DNI, pais (por defecto 'Argentina') y el nombre del comercio.")
    public ClientPointsResponse getPointsBalance(ClientPointsRequest request) {
        String country = (request.country() == null || request.country().isBlank()) ? "Argentina" : request.country();
        Optional<Client> clientOpt = clientRepository.findByDniAndCountry(request.dni(), country);

        if (clientOpt.isEmpty()) {
            return new ClientPointsResponse(false, null, request.companyName(), 0, "No se encontro ning n cliente registrado con el DNI proporcionado.");
        }
        Client client = clientOpt.get();

        Optional<Company> companyOpt = companyRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(request.companyName().trim()))
                .findFirst();

        if (companyOpt.isEmpty()) {
            return new ClientPointsResponse(false, client.getName(), request.companyName(), 0, "No se encontro el comercio o empresa indicado.");
        }
        Company company = companyOpt.get();

        Optional<PointsAccount> accountOpt = pointsAccountRepository.findByClientIdAndCompanyId(client.getId(), company.getId());
        if (accountOpt.isEmpty()) {
            return new ClientPointsResponse(false, client.getName(), company.getName(), 0, "El cliente no posee una cuenta de puntos activa en este comercio.");
        }

        return new ClientPointsResponse(true, client.getName(), company.getName(), accountOpt.get().getBalance(), "Consulta de saldo exitosa.");
    }

    @Tool(description = "Obtiene el catalogo de premios y recompensas activos disponibles para canjear en un comercio o empresa especifico ingresando su nombre.")
    public RewardsResponse getAvailableRewards(RewardsRequest request) {
        Optional<Company> companyOpt = companyRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(request.companyName().trim()))
                .findFirst();

        if (companyOpt.isEmpty()) {
            return new RewardsResponse(false, request.companyName(), List.of(), "No se encontro el comercio.");
        }
        Company company = companyOpt.get();

        List<Reward> rewards = rewardRepository.findAll().stream()
                .filter(r -> r.getCompany().getId().equals(company.getId()) && Boolean.TRUE.equals(r.getIsEnabled()))
                .toList();

        List<RewardInfo> rewardInfos = rewards.stream()
                .map(r -> new RewardInfo(r.getName(), r.getDescription(), r.getCostInPoints()))
                .toList();

        return new RewardsResponse(true, company.getName(), rewardInfos, "Premios obtenidos correctamente.");
    }

    @Tool(description = "Obtiene las promociones de multiplicadores de puntos (ejemplo: 2x) vigentes en un comercio especifico ingresando su nombre.")
    public PromotionResponse getActivePromotions(PromotionRequest request) {
        Optional<Company> companyOpt = companyRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(request.companyName().trim()))
                .findFirst();

        if (companyOpt.isEmpty()) {
            return new PromotionResponse(false, request.companyName(), null, "Comercio no encontrado.", 1.0);
        }
        Company company = companyOpt.get();

        Optional<Promotion> promoOpt = promotionRepository.findActivePromotion(company.getId(), OffsetDateTime.now(ZoneOffset.UTC));
        if (promoOpt.isEmpty()) {
            return new PromotionResponse(false, company.getName(), "Sin promociones activas", "No hay multiplicadores vigentes actualmente.", 1.0);
        }
        Promotion promo = promoOpt.get();

        return new PromotionResponse(true, company.getName(), promo.getName(), promo.getDescription(), promo.getMultiplier().doubleValue());
    }
}