package com.tech.point_system.service;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system.config.SubscriptionPlanProperties;
import com.tech.point_system.dto.subscription.PlanConfigDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPlanConfigService {

    private final SubscriptionPlanProperties properties;

    public List<PlanConfigDTO> getCommercialPlans() {
        return List.of(
                getPlanConfig(SubscriptionPlan.BASIC),
                getPlanConfig(SubscriptionPlan.PRO),
                getPlanConfig(SubscriptionPlan.ENTERPRISE)
        );
    }

    public PlanConfigDTO getPlanConfig(SubscriptionPlan plan) {
        if (plan == null || plan == SubscriptionPlan.NONE || plan == SubscriptionPlan.FREE_TRIAL) {
            return getDefaultFreePlan(plan);
        }

        String key = plan.name().toLowerCase(Locale.ROOT);
        SubscriptionPlanProperties.PlanItemProperties props = properties.getPlans().get(key);

        if (props == null || props.getPriceMonthlyArs() == null || props.getPriceMonthlyArs().compareTo(BigDecimal.ZERO) == 0) {
            return getDefaultCommercialPlan(plan);
        }

        return new PlanConfigDTO(
                plan,
                props.getName(),
                props.getTagline(),
                props.getPriceMonthlyArs(),
                props.getPriceYearlyArs(),
                props.getPriceMonthlyUsd(),
                props.getPriceYearlyUsd(),
                props.getMaxClients(),
                props.getMaxRewards(),
                props.getMaxCompanies(),
                props.isCanCreatePromotions(),
                props.isPopular(),
                props.getFeatures() != null && !props.getFeatures().isEmpty() ? props.getFeatures() : getDefaultCommercialPlan(plan).features()
        );
    }

    public BigDecimal getPlanPrice(SubscriptionPlan plan, BillingPeriod period, String currency) {
        if (plan == null || plan == SubscriptionPlan.NONE || plan == SubscriptionPlan.FREE_TRIAL) {
            return BigDecimal.ZERO;
        }

        PlanConfigDTO config = getPlanConfig(plan);
        boolean isUsd = currency != null && currency.equalsIgnoreCase("USD");

        if (period == BillingPeriod.YEARLY) {
            return isUsd ? config.priceYearlyUsd() : config.priceYearlyArs();
        } else {
            return isUsd ? config.priceMonthlyUsd() : config.priceMonthlyArs();
        }
    }

    private PlanConfigDTO getDefaultFreePlan(SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.FREE_TRIAL) {
            return new PlanConfigDTO(
                    SubscriptionPlan.FREE_TRIAL,
                    "Prueba Gratuita",
                    "Periodo de prueba activo de 30 días sin costo.",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    100,
                    5,
                    1,
                    true,
                    false,
                    List.of("1 sucursal comercial", "Hasta 100 clientes registrados", "Hasta 5 premios o recompensas", "Campañas y promociones incluidas")
            );
        }
        return new PlanConfigDTO(
                SubscriptionPlan.NONE,
                "Sin Plan Activo",
                "No posees un plan de suscripción activo ni periodo de prueba.",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                0,
                false,
                false,
                List.of("Sin sucursales habilitadas", "Sin clientes habilitados", "Requiere contratar un plan para operar")
        );
    }

    private PlanConfigDTO getDefaultCommercialPlan(SubscriptionPlan plan) {
        return switch (plan) {
            case BASIC -> new PlanConfigDTO(
                    SubscriptionPlan.BASIC,
                    "Plan Emprendedor",
                    "Ideal para pequeños locales o comercios en etapa inicial.",
                    new BigDecimal("9900.00"),
                    new BigDecimal("99000.00"),
                    new BigDecimal("15.00"),
                    new BigDecimal("150.00"),
                    100,
                    5,
                    1,
                    false,
                    false,
                    List.of(
                            "Hasta 100 clientes registrados",
                            "Hasta 5 premios o recompensas",
                            "1 sucursal comercial",
                            "Catálogo básico de productos",
                            "App móvil para escaneo QR",
                            "Soporte por Email"
                    )
            );
            case PRO -> new PlanConfigDTO(
                    SubscriptionPlan.PRO,
                    "Plan Crecimiento",
                    "Para marcas en expansión que buscan automatizar su fidelización.",
                    new BigDecimal("19900.00"),
                    new BigDecimal("199000.00"),
                    new BigDecimal("29.00"),
                    new BigDecimal("290.00"),
                    1000,
                    -1,
                    3,
                    true,
                    true,
                    List.of(
                            "Hasta 1,000 clientes registrados",
                            "Premios y recompensas ilimitados",
                            "Hasta 3 sucursales comerciales",
                            "Campañas y promociones con multiplicadores (2x, 3x)",
                            "Analytics avanzado y reportes",
                            "Soporte prioritario por WhatsApp"
                    )
            );
            case ENTERPRISE -> new PlanConfigDTO(
                    SubscriptionPlan.ENTERPRISE,
                    "Plan Corporativo",
                    "Franquicias o cadenas con múltiples sucursales y alto volumen.",
                    new BigDecimal("39900.00"),
                    new BigDecimal("399000.00"),
                    new BigDecimal("59.00"),
                    new BigDecimal("590.00"),
                    -1,
                    -1,
                    -1,
                    true,
                    false,
                    List.of(
                            "Clientes ilimitados",
                            "Premios y recompensas ilimitados",
                            "Sucursales y empresas ilimitadas",
                            "Campañas y promociones ilimitadas",
                            "Marca blanca y dominio personalizado",
                            "API custom de integración",
                            "Soporte 24/7 y ejecutivo de cuenta dedicado"
                    )
            );
            default -> getDefaultFreePlan(plan);
        };
    }
}
