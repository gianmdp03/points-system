package com.tech.point_system.config;

import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.model.Client;
import com.tech.point_system.model.Company;
import com.tech.point_system.model.PointsAccount;
import com.tech.point_system.model.Promotion;
import com.tech.point_system.model.Reward;
import com.tech.point_system.repository.*;
import com.tech.point_system.service.PlanValidatorService;
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
    private final PlanValidatorService planValidatorService;

    public record ClientPointsRequest(String dni, String country, String companyName) {}
    public record ClientPointsResponse(
            boolean success,
            String clientName,
            String companyName,
            Integer balance,
            boolean isPointsExpirationEnabled,
            Integer pointsExpirationDays,
            boolean isInactiveClientPurgeEnabled,
            Integer inactiveClientPurgeDays,
            String message
    ) {}

    public record CompanyDetailsRequest(String companyName) {}
    public record CompanyDetailsResponse(
            boolean success,
            String companyName,
            String accumulationRule,
            String pointsExpirationPolicy,
            String inactiveClientPurgePolicy,
            int activeRewardsCount,
            String activePromotionSummary,
            String message
    ) {}

    public record RewardsRequest(String companyName) {}
    public record RewardInfo(String name, String description, Integer costInPoints) {}
    public record RewardsResponse(boolean success, String companyName, List<RewardInfo> rewards, String message) {}

    public record PromotionRequest(String companyName) {}
    public record PromotionResponse(boolean hasActivePromotion, String companyName, String promotionName, String description, Double multiplier) {}

    public record SubscriptionPlanDetails(
            String plan,
            String name,
            String tagline,
            String priceUsd,
            String priceArs,
            String maxClients,
            String maxRewards,
            String maxCompanies,
            boolean canCreatePromotions,
            List<String> features
    ) {}
    public record SubscriptionPlansRequest(String specificPlan) {}
    public record SubscriptionPlansResponse(
            boolean success,
            List<SubscriptionPlanDetails> plans,
            String checkoutInfo,
            String message
    ) {}

    @Tool(description = "Consulta el saldo actual de puntos de un cliente en un comercio especifico con su DNI, pais (por defecto 'Argentina') y el nombre del comercio. Ademas informa si los puntos tienen fecha de vencimiento o si el comercio aplica baja por inactividad.")
    public ClientPointsResponse getPointsBalance(ClientPointsRequest request) {
        String country = (request.country() == null || request.country().isBlank()) ? "Argentina" : request.country();
        Optional<Client> clientOpt = clientRepository.findByDniAndCountry(request.dni(), country);

        if (clientOpt.isEmpty()) {
            return new ClientPointsResponse(false, null, request.companyName(), 0, false, null, false, null, "No se encontro ningun cliente registrado con el DNI proporcionado.");
        }
        Client client = clientOpt.get();

        String companyName = request.companyName() != null ? request.companyName().trim() : "";
        Optional<Company> companyOpt = companyRepository.findByNameIgnoreCase(companyName);

        if (companyOpt.isEmpty()) {
            return new ClientPointsResponse(false, client.getName(), request.companyName(), 0, false, null, false, null, "No se encontro el comercio indicado.");
        }
        Company company = companyOpt.get();

        Optional<PointsAccount> accountOpt = pointsAccountRepository.findByClientIdAndCompanyId(client.getId(), company.getId());
        if (accountOpt.isEmpty()) {
            return new ClientPointsResponse(false, client.getName(), company.getName(), 0,
                    Boolean.TRUE.equals(company.getIsPointsExpirationEnabled()), company.getPointsExpirationDays(),
                    Boolean.TRUE.equals(company.getIsInactiveClientPurgeEnabled()), company.getInactiveClientPurgeDays(),
                    "El cliente no posee una cuenta de puntos asociada en este comercio.");
        }

        return new ClientPointsResponse(
                true,
                client.getName(),
                company.getName(),
                accountOpt.get().getBalance(),
                Boolean.TRUE.equals(company.getIsPointsExpirationEnabled()),
                company.getPointsExpirationDays(),
                Boolean.TRUE.equals(company.getIsInactiveClientPurgeEnabled()),
                company.getInactiveClientPurgeDays(),
                "Saldo y politicas consultadas con exito."
        );
    }

    @Tool(description = "Obtiene los detalles completos de fidelizacion de un comercio ingresando su nombre: como acumular puntos ($ por punto), politicas de vencimiento de puntos, politicas de baja de clientes inactivos, cantidad de premios y promociones activas.")
    public CompanyDetailsResponse getCompanyDetails(CompanyDetailsRequest request) {
        String companyName = request.companyName() != null ? request.companyName().trim() : "";
        Optional<Company> companyOpt = companyRepository.findByNameIgnoreCase(companyName);

        if (companyOpt.isEmpty()) {
            return new CompanyDetailsResponse(false, request.companyName(), null, null, null, 0, null, "Comercio no encontrado.");
        }
        Company company = companyOpt.get();

        String rule = "$" + company.getAmountStep() + " gastados = " + company.getPointsPerStep() + " puntos acumulados";
        String expiration = Boolean.TRUE.equals(company.getIsPointsExpirationEnabled()) && company.getPointsExpirationDays() != null
                ? "Los puntos vencen a los " + company.getPointsExpirationDays() + " dias desde su fecha de emision (los puntos mas antiguos vencen primero)."
                : "Los puntos acumulados no vencen.";

        String purge = Boolean.TRUE.equals(company.getIsInactiveClientPurgeEnabled()) && company.getInactiveClientPurgeDays() != null
                ? "Las cuentas sin actividad (sumas o canjes) durante " + company.getInactiveClientPurgeDays() + " dias son dadas de baja automaticamente para optimizar los cupos del comercio."
                : "Las cuentas de clientes se mantienen activas de forma indefinida.";

        long rewardsCount = rewardRepository.countByCompanyId(company.getId());

        Optional<Promotion> promoOpt = promotionRepository.findActivePromotion(company.getId(), OffsetDateTime.now(ZoneOffset.UTC));
        String promoSummary = promoOpt.map(p -> p.getName() + " (" + p.getMultiplier() + "x en puntos) - " + p.getDescription())
                .orElse("No hay promociones multiplicadoras activas en este momento.");

        return new CompanyDetailsResponse(
                true,
                company.getName(),
                rule,
                expiration,
                purge,
                (int) rewardsCount,
                promoSummary,
                "Informacion del comercio obtenida con exito."
        );
    }

    @Tool(description = "Obtiene el catalogo de premios y recompensas activos disponibles para canjear en un comercio o empresa especifico ingresando su nombre.")
    public RewardsResponse getAvailableRewards(RewardsRequest request) {
        String companyName = request.companyName() != null ? request.companyName().trim() : "";
        Optional<Company> companyOpt = companyRepository.findByNameIgnoreCase(companyName);

        if (companyOpt.isEmpty()) {
            return new RewardsResponse(false, request.companyName(), List.of(), "No se encontro el comercio.");
        }
        Company company = companyOpt.get();

        List<Reward> rewards = rewardRepository.findByCompanyIdAndIsEnabledTrue(company.getId());

        List<RewardInfo> rewardInfos = rewards.stream()
                .map(r -> new RewardInfo(r.getName(), r.getDescription(), r.getCostInPoints()))
                .toList();

        return new RewardsResponse(true, company.getName(), rewardInfos, "Premios obtenidos correctamente.");
    }

    @Tool(description = "Obtiene las promociones de multiplicadores de puntos (ejemplo: 2x, 3x) vigentes en un comercio especifico ingresando su nombre.")
    public PromotionResponse getActivePromotions(PromotionRequest request) {
        String companyName = request.companyName() != null ? request.companyName().trim() : "";
        Optional<Company> companyOpt = companyRepository.findByNameIgnoreCase(companyName);

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

    @Tool(description = "Consulta la informacion oficial, dinamica y actualizada en tiempo real de los planes de suscripcion de Pointly (BASIC, PRO, ENTERPRISE, FREE_TRIAL) incluyendo precios en USD/ARS, limites de clientes, limites de sucursales, limites de premios y permisos de creacion de promociones. Usala SIEMPRE que los usuarios o comercios pregunten por planes, tarifas, limites, comparaciones entre planes, como mejorar de plan o precios.")
    public SubscriptionPlansResponse getSubscriptionPlansInfo(SubscriptionPlansRequest request) {
        PlanConfigProperties.PlanLimits basicLimits = planValidatorService.getLimitsForPlan(SubscriptionPlan.BASIC);
        PlanConfigProperties.PlanLimits proLimits = planValidatorService.getLimitsForPlan(SubscriptionPlan.PRO);
        PlanConfigProperties.PlanLimits entLimits = planValidatorService.getLimitsForPlan(SubscriptionPlan.ENTERPRISE);
        PlanConfigProperties.PlanLimits trialLimits = planValidatorService.getLimitsForPlan(SubscriptionPlan.FREE_TRIAL);

        List<SubscriptionPlanDetails> allPlans = List.of(
                new SubscriptionPlanDetails(
                        "FREE_TRIAL",
                        "Prueba Gratuita",
                        "Periodo de prueba activo de 30 dias para nuevos comercios.",
                        "$0 USD",
                        "$0 ARS",
                        trialLimits.getMaxClients() == -1 ? "Ilimitados" : String.valueOf(trialLimits.getMaxClients()),
                        trialLimits.getMaxRewards() == -1 ? "Ilimitados" : String.valueOf(trialLimits.getMaxRewards()),
                        trialLimits.getMaxCompanies() == -1 ? "Ilimitadas" : String.valueOf(trialLimits.getMaxCompanies()),
                        trialLimits.isCanCreatePromotions(),
                        List.of("Prueba completa de 30 dias sin cargo", "Hasta " + trialLimits.getMaxClients() + " clientes", "Promociones con multiplicadores habilitadas")
                ),
                new SubscriptionPlanDetails(
                        "BASIC",
                        "Plan Emprendedor",
                        "Ideal para pequeños locales o comercios en etapa inicial.",
                        "$19 USD/mes",
                        "$9.900 ARS/mes",
                        basicLimits.getMaxClients() == -1 ? "Ilimitados" : String.valueOf(basicLimits.getMaxClients()),
                        basicLimits.getMaxRewards() == -1 ? "Ilimitados" : String.valueOf(basicLimits.getMaxRewards()),
                        basicLimits.getMaxCompanies() == -1 ? "Ilimitadas" : String.valueOf(basicLimits.getMaxCompanies()),
                        basicLimits.isCanCreatePromotions(),
                        List.of(
                                "Hasta " + (basicLimits.getMaxClients() == -1 ? "Ilimitados" : basicLimits.getMaxClients()) + " clientes registrados",
                                "Hasta " + (basicLimits.getMaxRewards() == -1 ? "Ilimitados" : basicLimits.getMaxRewards()) + " premios",
                                basicLimits.getMaxCompanies() + " sucursal comercial",
                                "App movil para escaneo QR",
                                basicLimits.isCanCreatePromotions() ? "Promociones habilitadas" : "No incluye promociones multiplicadoras"
                        )
                ),
                new SubscriptionPlanDetails(
                        "PRO",
                        "Plan Crecimiento",
                        "Para marcas en expansion que buscan automatizar su fidelizacion.",
                        "$49 USD/mes",
                        "$19.900 ARS/mes",
                        proLimits.getMaxClients() == -1 ? "Ilimitados" : String.valueOf(proLimits.getMaxClients()),
                        proLimits.getMaxRewards() == -1 ? "Ilimitados" : String.valueOf(proLimits.getMaxRewards()),
                        proLimits.getMaxCompanies() == -1 ? "Ilimitadas" : String.valueOf(proLimits.getMaxCompanies()),
                        proLimits.isCanCreatePromotions(),
                        List.of(
                                "Hasta " + (proLimits.getMaxClients() == -1 ? "Ilimitados" : proLimits.getMaxClients()) + " clientes registrados",
                                (proLimits.getMaxRewards() == -1 ? "Premios y recompensas ilimitados" : "Hasta " + proLimits.getMaxRewards() + " premios"),
                                "Hasta " + (proLimits.getMaxCompanies() == -1 ? "Ilimitadas" : proLimits.getMaxCompanies()) + " sucursales comerciales",
                                "Campañas y promociones con multiplicadores (2x, 3x)",
                                "Analytics avanzado y soporte prioritario"
                        )
                ),
                new SubscriptionPlanDetails(
                        "ENTERPRISE",
                        "Plan Corporativo",
                        "Franquicias o cadenas con multiples sucursales y alto volumen.",
                        "$99 USD/mes",
                        "$39.900 ARS/mes",
                        entLimits.getMaxClients() == -1 ? "Ilimitados" : String.valueOf(entLimits.getMaxClients()),
                        entLimits.getMaxRewards() == -1 ? "Ilimitados" : String.valueOf(entLimits.getMaxRewards()),
                        entLimits.getMaxCompanies() == -1 ? "Ilimitadas" : String.valueOf(entLimits.getMaxCompanies()),
                        entLimits.isCanCreatePromotions(),
                        List.of(
                                "Clientes ilimitados",
                                "Premios y recompensas ilimitados",
                                "Sucursales y empresas ilimitadas",
                                "Campañas y promociones ilimitadas",
                                "Marca blanca y API de integracion",
                                "Soporte 24/7 y ejecutivo de cuenta dedicado"
                        )
                )
        );

        if (request != null && request.specificPlan() != null && !request.specificPlan().isBlank()) {
            String planFilter = request.specificPlan().trim().toUpperCase();
            List<SubscriptionPlanDetails> filtered = allPlans.stream()
                    .filter(p -> p.plan().equalsIgnoreCase(planFilter) || p.name().toUpperCase().contains(planFilter))
                    .toList();
            if (!filtered.isEmpty()) {
                return new SubscriptionPlansResponse(
                        true,
                        filtered,
                        "Los comercios pueden suscribirse o cambiar de plan desde la seccion /pricing o en Mi Panel > Planes.",
                        "Detalle del plan " + planFilter + " obtenido exitosamente."
                );
            }
        }

        return new SubscriptionPlansResponse(
                true,
                allPlans,
                "Los comercios pueden suscribirse o cambiar de plan desde la seccion /pricing o en Mi Panel > Planes.",
                "Planes de suscripcion obtenidos exitosamente."
        );
    }
}
