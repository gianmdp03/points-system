package com.tech.point_system.payment.impl;

import com.tech.point_system._enum.BillingPeriod;
import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system._enum.SubscriptionPlan;
import com.tech.point_system._enum.SubscriptionStatus;
import com.tech.point_system.config.MercadoPagoProperties;
import com.tech.point_system.dto.mercadopago.MpPreferenceModels.*;
import com.tech.point_system.dto.subscription.PlanConfigDTO;
import com.tech.point_system.dto.subscription.ProrationPreviewResponseDTO;
import com.tech.point_system.dto.subscription.SubscriptionRequestDTO;
import com.tech.point_system.dto.subscription.SubscriptionResponseDTO;
import com.tech.point_system.dto.subscription.SubscriptionUpgradeRequestDTO;
import com.tech.point_system.exception.PaymentGatewayException;
import com.tech.point_system.model.Subscription;
import com.tech.point_system.model.User;
import com.tech.point_system.payment.PaymentStrategy;
import com.tech.point_system.payment.mercadopago.MercadoPagoPreferenceClient;
import com.tech.point_system.model.Company;
import com.tech.point_system.repository.CompanyRepository;
import com.tech.point_system.repository.SubscriptionRepository;
import com.tech.point_system.repository.UserRepository;
import com.tech.point_system.service.ProrationCalculatorService;
import com.tech.point_system.service.SubscriptionPlanConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoPaymentStrategy implements PaymentStrategy {

    private final MercadoPagoProperties properties;
    private final MercadoPagoPreferenceClient preferenceClient;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ProrationCalculatorService prorationCalculatorService;
    private final SubscriptionPlanConfigService planConfigService;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MERCADO_PAGO;
    }

    @Override
    @Transactional
    public SubscriptionResponseDTO createSubscription(User user, SubscriptionRequestDTO dto) {
        log.info("[CHECKOUT PRO] 🚀 [CREAR PREFERENCIA DE SUSCRIPCIÓN] Usuario='{}' ({}) | Plan Solicitado='{}' | Periodo='{}'",
                user.getId(), user.getEmail(), dto.plan(), dto.billingPeriod());

        if (dto.plan() == SubscriptionPlan.NONE || dto.plan() == SubscriptionPlan.FREE_TRIAL) {
            log.warn("[CHECKOUT PRO] ⚠️ Intento de cobro para plan sin costo '{}'. Rechazando.", dto.plan());
            throw new PaymentGatewayException("rejected", "free_plan", "No se puede crear un cobro de Mercado Pago para planes sin costo.", 400);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String currency = "ARS";
        BigDecimal planPrice = planConfigService.getPlanPrice(dto.plan(), dto.billingPeriod(), currency);
        PlanConfigDTO planConfig = planConfigService.getPlanConfig(dto.plan());

        boolean isRegularization = (user.getPendingDebtArs() != null && user.getPendingDebtArs().compareTo(BigDecimal.ZERO) > 0)
                || Boolean.TRUE.equals(user.getIsSuspendedForChargeback());

        BigDecimal finalPrice;
        String itemTitle;
        String itemDescription;
        String externalRef;

        if (isRegularization) {
            BigDecimal pendingDebt = user.getPendingDebtArs() != null ? user.getPendingDebtArs() : BigDecimal.ZERO;
            boolean hasFutureLegitimateDays = user.getPlanExpirationDate() != null && user.getPlanExpirationDate().isAfter(now);

            if (hasFutureLegitimateDays) {
                finalPrice = pendingDebt;
                itemTitle = "Regularización de Saldo Deudor - Pointly";
                itemDescription = "Cancelación de deuda acumulada por contracargo";
            } else {
                finalPrice = pendingDebt.add(planPrice);
                itemTitle = "Regularización de Deuda + Suscripción " + planConfig.name();
                itemDescription = "Cancelación de deuda pendiente y reactivación de plan";
            }

            externalRef = String.format("REC:%s:%s:%s:%s",
                    user.getId(),
                    dto.plan().name(),
                    dto.billingPeriod().name(),
                    UUID.randomUUID()
            );

            log.info("[CHECKOUT PRO] 💳 [REGULARIZACIÓN] Usuario '{}' regularizando deuda de {} ARS (Total a cobrar: {} ARS, Días legítimos futuros: {}) | ExternalRef='{}'",
                    user.getId(), pendingDebt, finalPrice, hasFutureLegitimateDays, externalRef);
        } else {
            finalPrice = planPrice;
            itemTitle = "Suscripción Pointly - " + planConfig.name();
            itemDescription = planConfig.tagline();

            externalRef = String.format("SUB:%s:%s:%s:%s",
                    user.getId(),
                    dto.plan().name(),
                    dto.billingPeriod().name(),
                    UUID.randomUUID()
            );
        }

        String backUrl = properties.getBackUrl();
        String userName = StringUtils.hasText(user.getName()) ? user.getName() : "Usuario";
        String payerEmail = properties.isSandbox() ? "test_user_buyer@testuser.com" : (StringUtils.hasText(user.getEmail()) ? user.getEmail() : "cliente@pointly.app");
        String autoReturn = (StringUtils.hasText(backUrl) && backUrl.startsWith("https://") && !backUrl.contains("localhost")) ? "approved" : null;

        MpPreferenceIdentification identification = (user != null && StringUtils.hasText(user.getDni()))
                ? new MpPreferenceIdentification("DNI", user.getDni())
                : null;

        MpPreferenceRequest preferenceRequest = new MpPreferenceRequest(
                List.of(new MpPreferenceItem(
                        "plan-" + dto.plan().name().toLowerCase(),
                        itemTitle,
                        itemDescription,
                        "services",
                        1,
                        finalPrice,
                        currency
                )),
                new MpPreferencePayer(
                        userName,
                        "Pointly",
                        payerEmail,
                        identification
                ),
                new MpPreferenceBackUrls(
                        backUrl + "?status=approved",
                        backUrl + "?status=pending",
                        backUrl + "?status=failure"
                ),
                autoReturn,
                null,
                externalRef,
                "POINTLY",
                false
        );

        try {
            MpPreferenceResponse prefResponse = preferenceClient.createPreference(preferenceRequest);
            String checkoutUrl = properties.isSandbox() && StringUtils.hasText(prefResponse.sandboxInitPoint())
                    ? prefResponse.sandboxInitPoint()
                    : prefResponse.initPoint();

            log.info("[CHECKOUT PRO] ✅ Preferencia creada exitosamente. CheckoutUrl='{}' | ExternalRef='{}'",
                    checkoutUrl, externalRef);

            return new SubscriptionResponseDTO(
                    null,
                    dto.plan(),
                    SubscriptionStatus.PENDING,
                    PaymentProvider.MERCADO_PAGO,
                    finalPrice,
                    currency,
                    checkoutUrl,
                    externalRef
            );

        } catch (PaymentGatewayException pge) {
            throw pge;
        } catch (Exception e) {
            log.error("[CHECKOUT PRO] ❌ Error creando preferencia de Checkout Pro para usuario '{}'", user.getId(), e);
            throw new PaymentGatewayException("rejected", "preference_error", "Error al generar la preferencia de pago en Mercado Pago: " + e.getMessage(), 400);
        }
    }

    @Override
    @Transactional
    public SubscriptionResponseDTO upgradeSubscription(
            Subscription currentSubscription,
            SubscriptionPlan newPlan,
            SubscriptionUpgradeRequestDTO upgradeDTO) {

        log.info("[CHECKOUT PRO] 🔄 [UPGRADE] Generando preferencia de upgrade de '{}' a '{}' para suscripción ID: {}",
                currentSubscription.getPlan(), newPlan, currentSubscription.getId());

        ProrationPreviewResponseDTO proration = prorationCalculatorService.calculateUpgradeProration(currentSubscription, newPlan);
        BigDecimal upgradeAmount = proration.proratedUpgradeAmount();

        String currency = currentSubscription.getCurrency() != null ? currentSubscription.getCurrency() : "ARS";
        PlanConfigDTO newPlanConfig = planConfigService.getPlanConfig(newPlan);

        // Formato estricto: UPG:{userId}:{targetPlan}:{currentSubId}:{UUID}
        String externalRef = String.format("UPG:%s:%s:%d:%s",
                currentSubscription.getUser().getId(),
                newPlan.name(),
                currentSubscription.getId(),
                UUID.randomUUID()
        );

        String backUrl = properties.getBackUrl();
        User user = currentSubscription.getUser();
        String userName = (user != null && StringUtils.hasText(user.getName())) ? user.getName() : "Usuario";
        String userEmail = user != null ? user.getEmail() : "usuario@pointly.com";
        // auto_return solo es válido en Mercado Pago con dominios públicos HTTPS reales (rechaza http:// y localhost)
        String autoReturn = (StringUtils.hasText(backUrl) && backUrl.startsWith("https://") && !backUrl.contains("localhost")) ? "approved" : null;

        MpPreferenceIdentification identification = (user != null && StringUtils.hasText(user.getDni()))
                ? new MpPreferenceIdentification("DNI", user.getDni())
                : null;

        // Omitir notificationUrl para delegar 100% a Webhooks v2 del Dashboard
        MpPreferenceRequest preferenceRequest = new MpPreferenceRequest(
                List.of(new MpPreferenceItem(
                        "upgrade-" + newPlan.name().toLowerCase(),
                        "Upgrade a Plan " + newPlanConfig.name() + " (Días restantes)",
                        "Cobro diferencial por upgrade prorrateado",
                        "services",
                        1,
                        upgradeAmount,
                        currency
                )),
                new MpPreferencePayer(
                        userName,
                        "Pointly",
                        properties.isSandbox() ? "test_user_buyer@testuser.com" : userEmail,
                        identification
                ),
                new MpPreferenceBackUrls(
                        backUrl + "?status=approved",
                        backUrl + "?status=pending",
                        backUrl + "?status=failure"
                ),
                autoReturn,
                null,
                externalRef,
                "POINTLY",
                false
        );


        try {
            MpPreferenceResponse prefResponse = preferenceClient.createPreference(preferenceRequest);
            String checkoutUrl = properties.isSandbox() && StringUtils.hasText(prefResponse.sandboxInitPoint())
                    ? prefResponse.sandboxInitPoint()
                    : prefResponse.initPoint();

            log.info("[CHECKOUT PRO] ✅ Preferencia de Upgrade creada. CheckoutUrl='{}' | ExternalRef='{}' | Monto={}",
                    checkoutUrl, externalRef, upgradeAmount);

            return new SubscriptionResponseDTO(
                    currentSubscription.getId(),
                    newPlan,
                    SubscriptionStatus.PENDING,
                    PaymentProvider.MERCADO_PAGO,
                    upgradeAmount,
                    currency,
                    checkoutUrl,
                    externalRef
            );

        } catch (PaymentGatewayException pge) {
            throw pge;
        } catch (Exception e) {
            log.error("[CHECKOUT PRO] ❌ Error generando preferencia de Upgrade", e);
            throw new PaymentGatewayException("rejected", "upgrade_preference_error",
                    "Error al generar la preferencia de cobro prorrateado del Upgrade: " + e.getMessage(), 400);
        }
    }

    @Override
    public SubscriptionResponseDTO upgradeSubscription(Subscription currentSubscription, SubscriptionPlan newPlan) {
        return upgradeSubscription(currentSubscription, newPlan, null);
    }


    @Override
    @Transactional
    public void processWebhook(Map<String, Object> payload) {
        log.info("[CHECKOUT PRO WEBHOOK] 🔔 [EVENTO RECIBIDO] Procesando notificación: {}", payload);

        String topic = extractTopic(payload);
        String entityId = extractEntityId(payload);

        if (!StringUtils.hasText(entityId)) {
            log.warn("[CHECKOUT PRO WEBHOOK] ⚠️ No se encontró 'id' o 'data.id' en el payload recibido: {}", payload);
            return;
        }

        boolean isWebhookChargeback = isChargebackEvent(payload);
        boolean isWebhookRefund = isRefundEvent(payload);

        // Si es notificación de orden comercial (Checkout Pro IPN o Webhook)
        if (topic.contains("merchant_order") || topic.contains("merchant-order")) {
            log.info("[CHECKOUT PRO WEBHOOK] 📦 Notificación de Merchant Order #{}", entityId);
            com.tech.point_system.dto.mercadopago.MpPreferenceModels.MpMerchantOrderResponse order = preferenceClient.getMerchantOrder(entityId);
            if (order != null && order.payments() != null && !order.payments().isEmpty()) {
                for (var orderPayment : order.payments()) {
                    if (orderPayment.id() != null) {
                        processSinglePayment(orderPayment.id().toString(), isWebhookChargeback, isWebhookRefund);
                    }
                }
            } else {
                log.info("[CHECKOUT PRO WEBHOOK] ℹ️ Merchant Order #{} sin pagos aprobados todavía (estado opened/en curso).", entityId);
            }
            return;
        }

        // Si es notificación directa de pago
        processSinglePayment(entityId, isWebhookChargeback, isWebhookRefund);
    }

    private void processSinglePayment(String paymentId, boolean isWebhookChargeback, boolean isWebhookRefund) {
        log.info("[CHECKOUT PRO WEBHOOK] 🆔 ID de Pago: '{}' (WebhookChargeback={}, WebhookRefund={}). Consultando en Mercado Pago API...",
                paymentId, isWebhookChargeback, isWebhookRefund);

        try {
            MpPaymentResponse payment = preferenceClient.getPayment(paymentId);
            if (payment == null) {
                log.warn("[CHECKOUT PRO WEBHOOK] ⚠️ Pago #{} no encontrado en Mercado Pago.", paymentId);
                return;
            }

            String status = payment.status() != null ? payment.status().toLowerCase() : "";
            String statusDetail = payment.statusDetail() != null ? payment.statusDetail().toLowerCase() : "";
            String externalRef = payment.externalReference();

            log.info("[CHECKOUT PRO WEBHOOK] 📋 Pago #{}: Status='{}' | Detail='{}' | Monto={} | ExternalRef='{}'",
                    payment.id(), status, statusDetail, payment.transactionAmount(), externalRef);

            if (!StringUtils.hasText(externalRef)) {
                log.warn("[CHECKOUT PRO WEBHOOK] ⚠️ Pago #{} no contiene 'external_reference'. No es posible asociarlo.", payment.id());
                return;
            }

            // Identificar si el pago fue revocado (contracargo bancario o reembolso)
            boolean isChargedBack = isWebhookChargeback
                    || "charged_back".equals(status)
                    || "charged_back".equals(statusDetail)
                    || (statusDetail != null && statusDetail.contains("chargeback"))
                    || (statusDetail != null && statusDetail.contains("charged_back"));

            boolean isRefunded = isWebhookRefund
                    || "refunded".equals(status)
                    || "refunded".equals(statusDetail)
                    || "partially_refunded".equals(statusDetail)
                    || (statusDetail != null && statusDetail.contains("refund"));

            boolean isRevoked = isChargedBack || isRefunded;

            // REGLA 4: Idempotencia Estricta
            if (isPaymentAlreadyProcessed(paymentId, isRevoked, status, externalRef)) {
                log.info("[CHECKOUT PRO WEBHOOK] ℹ️ Pago #{} con estado '{}/{}' ya fue procesado previamente. Omitiendo duplicación de lógica.",
                        paymentId, status, statusDetail);
                return;
            }

            // Procesamiento de Matriz Exhaustiva de Estados
            if (isRevoked) {
                handlePaymentRevoked(payment, statusDetail, externalRef, isChargedBack);
            } else {
                switch (status) {
                    case "approved" -> handlePaymentApproved(payment, statusDetail, externalRef);
                    case "in_process", "pending" -> handlePaymentPending(payment, statusDetail, externalRef);
                    case "rejected" -> handlePaymentRejected(payment, statusDetail, externalRef);
                    case "cancelled" -> handlePaymentCancelled(payment, statusDetail, externalRef);
                    default -> log.warn("[CHECKOUT PRO WEBHOOK] ⚠️ Estado desconocido '{}' (detail: '{}') para pago #{}", status, statusDetail, payment.id());
                }
            }

        } catch (Exception e) {
            log.error("[CHECKOUT PRO WEBHOOK] ❌ Error procesando evento para pago '{}'", paymentId, e);
        }
    }

    private boolean isChargebackEvent(Map<String, Object> payload) {
        if (payload == null) return false;
        String raw = payload.toString().toLowerCase();
        return raw.contains("chargeback")
                || raw.contains("topic_chargebacks_wh")
                || raw.contains("contracargo");
    }

    private boolean isRefundEvent(Map<String, Object> payload) {
        if (payload == null) return false;
        String raw = payload.toString().toLowerCase();
        return raw.contains("topic_refunds_wh")
                || raw.contains("refund.created")
                || raw.contains("refunds");
    }


    private boolean isPaymentAlreadyProcessed(String paymentId, boolean isRevoked, String status, String externalRef) {
        Subscription sub = subscriptionRepository.findByExternalSubscriptionId(paymentId)
                .or(() -> subscriptionRepository.findByExternalSubscriptionId(externalRef))
                .orElse(null);

        if (sub == null) {
            return false;
        }

        if (isRevoked) {
            return sub.getStatus() == SubscriptionStatus.EXPIRED;
        }

        if ("approved".equals(status)) {
            if (sub.getStatus() == SubscriptionStatus.APPROVED) {
                if (externalRef.startsWith("UPG:") || externalRef.startsWith("UPG_") || externalRef.startsWith("UPG-")) {
                    String[] parts = parseExternalReference(externalRef);
                    if (parts.length >= 3) {
                        try {
                            SubscriptionPlan targetPlan = SubscriptionPlan.valueOf(parts[2]);
                            return sub.getPlan() == targetPlan;
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                return true;
            }
            return false;
        } else if ("rejected".equals(status) || "cancelled".equals(status)) {
            return sub.getStatus() == SubscriptionStatus.PAYMENT_FAILED || sub.getStatus() == SubscriptionStatus.EXPIRED;
        }

        return false;
    }

    private void handlePaymentApproved(MpPaymentResponse payment, String statusDetail, String externalRef) {
        log.info("[CHECKOUT PRO WEBHOOK] 🟢 [PAGO APROBADO/ACREDITADO] Pago #{} acreditado ({}) | ExternalRef='{}'",
                payment.id(), statusDetail, externalRef);

        String[] parts = parseExternalReference(externalRef);

        if (externalRef.startsWith("SUB:") || externalRef.startsWith("SUB_") || externalRef.startsWith("SUB-")) {
            // Alta o Extensión de Días de Suscripción: SUB:{userId}:{plan}:{billingPeriod}:{UUID}
            if (parts.length >= 4) {
                String userId = parts[1];
                SubscriptionPlan plan = SubscriptionPlan.valueOf(parts[2]);
                BillingPeriod billingPeriod = BillingPeriod.valueOf(parts[3]);

                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    log.error("[CHECKOUT PRO WEBHOOK] ❌ Usuario '{}' no encontrado en BD para pago #{}", userId, payment.id());
                    return;
                }

                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                // Lógica de Bolsa de Días Acumulativos Prepago:
                // Si el usuario ya tiene el MISMO plan activo y con vigencia futura, sumamos los días a su vencimiento existente.
                OffsetDateTime baseDate = (user.getCurrentPlan() == plan
                        && user.getPlanExpirationDate() != null
                        && user.getPlanExpirationDate().isAfter(now))
                        ? user.getPlanExpirationDate()
                        : now;

                OffsetDateTime newExpiration = baseDate.plusDays(billingPeriod.getDays());

                user.setCurrentPlan(plan);
                user.setPlanExpirationDate(newExpiration);
                user.setIsFreeTrialOver(true);
                userRepository.save(user);

                Subscription subscription = subscriptionRepository.findByExternalSubscriptionId(payment.id().toString())
                        .orElseGet(() -> Subscription.builder()
                                .user(user)
                                .createdAt(now)
                                .build());

                subscription.setUser(user);
                subscription.setPlan(plan);
                subscription.setBillingPeriod(billingPeriod);
                subscription.setStatus(SubscriptionStatus.APPROVED);
                subscription.setProvider(PaymentProvider.MERCADO_PAGO);
                subscription.setPrice(payment.transactionAmount());
                subscription.setCurrency("ARS");
                subscription.setExternalSubscriptionId(payment.id().toString());
                subscription.setStartDate(now);

                subscriptionRepository.save(subscription);
                log.info("[CHECKOUT PRO WEBHOOK] ✅ [EXTENSIÓN/ACTIVACIÓN EXITOSA] Orden ID: {} para usuario '{}' con plan '{}' extendida hasta {}",
                        subscription.getId(), user.getEmail(), plan, newExpiration);
            }
        } else if (externalRef.startsWith("UPG:") || externalRef.startsWith("UPG_") || externalRef.startsWith("UPG-")) {
            // Upgrade Prorrateado: UPG:{userId}:{targetPlan}:{currentSubId}:{UUID}
            if (parts.length >= 3) {
                String userId = parts[1];
                SubscriptionPlan targetPlan = SubscriptionPlan.valueOf(parts[2]);

                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    log.error("[CHECKOUT PRO WEBHOOK] ❌ Usuario '{}' no encontrado en BD para upgrade de pago #{}", userId, payment.id());
                    return;
                }

                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                // Conservar la fecha de vencimiento existente (los días restantes que ya tenía contratados)
                user.setCurrentPlan(targetPlan);
                user.setIsFreeTrialOver(true);
                userRepository.save(user);

                Subscription subscription = subscriptionRepository.findByExternalSubscriptionId(payment.id().toString())
                        .orElseGet(() -> Subscription.builder()
                                .user(user)
                                .createdAt(now)
                                .build());

                subscription.setUser(user);
                subscription.setPlan(targetPlan);
                subscription.setBillingPeriod(subscription.getBillingPeriod() != null ? subscription.getBillingPeriod() : BillingPeriod.MONTHLY);
                subscription.setStatus(SubscriptionStatus.APPROVED);
                subscription.setProvider(PaymentProvider.MERCADO_PAGO);
                subscription.setPrice(payment.transactionAmount());
                subscription.setCurrency("ARS");
                subscription.setExternalSubscriptionId(payment.id().toString());
                subscription.setStartDate(now);

                subscriptionRepository.save(subscription);
                log.info("[CHECKOUT PRO WEBHOOK] ✅ [UPGRADE ACREDITADO] Orden ID: {} - Usuario '{}' actualizado a plan '{}' (Vencimiento conservado: {})",
                        subscription.getId(), user.getEmail(), targetPlan, user.getPlanExpirationDate());
            }
        } else if (externalRef.startsWith("REC:") || externalRef.startsWith("REC_") || externalRef.startsWith("REC-")) {
            // Regularización y Desbloqueo: REC:{userId}:{plan}:{billingPeriod}:{UUID}
            if (parts.length >= 4) {
                String userId = parts[1];
                SubscriptionPlan plan = SubscriptionPlan.valueOf(parts[2]);
                BillingPeriod billingPeriod = BillingPeriod.valueOf(parts[3]);

                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    log.error("[CHECKOUT PRO WEBHOOK] ❌ Usuario '{}' no encontrado en BD para regularización de pago #{}", userId, payment.id());
                    return;
                }

                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                // 1. Liquidar la deuda acumulada y levantar la suspensión
                user.setPendingDebtArs(BigDecimal.ZERO);
                user.setIsSuspendedForChargeback(false);

                // 2. Restaurar / Extender vigencia:
                // Si la fecha de vencimiento actual ya venció (<= now o nula), asignar now.plusDays(días) y el plan solicitado
                // Si todavía tenía días legítimos futuros a favor (> now), conservar esa fecha y reactivar el plan
                if (user.getPlanExpirationDate() == null || !user.getPlanExpirationDate().isAfter(now)) {
                    user.setPlanExpirationDate(now.plusDays(billingPeriod.getDays()));
                    user.setCurrentPlan(plan);
                } else if (user.getCurrentPlan() == SubscriptionPlan.NONE) {
                    user.setCurrentPlan(plan);
                }
                user.setIsFreeTrialOver(true);
                userRepository.save(user);

                // 3. Reactivar automáticamente todas las empresas del usuario
                List<Company> userCompanies = companyRepository.findAllByAdminId(user.getId());
                userCompanies.forEach(c -> {
                    c.setIsEnabled(true);
                    c.setDisabledDate(null);
                });
                companyRepository.saveAll(userCompanies);

                // 4. Guardar la orden de suscripción como APPROVED
                Subscription subscription = subscriptionRepository.findByExternalSubscriptionId(payment.id().toString())
                        .orElseGet(() -> Subscription.builder()
                                .user(user)
                                .createdAt(now)
                                .build());

                subscription.setUser(user);
                subscription.setPlan(user.getCurrentPlan());
                subscription.setBillingPeriod(billingPeriod);
                subscription.setStatus(SubscriptionStatus.APPROVED);
                subscription.setProvider(PaymentProvider.MERCADO_PAGO);
                subscription.setPrice(payment.transactionAmount());
                subscription.setCurrency("ARS");
                subscription.setExternalSubscriptionId(payment.id().toString());
                subscription.setStartDate(now);

                subscriptionRepository.save(subscription);
                log.info("[CHECKOUT PRO WEBHOOK] 🔓 [REGULARIZACIÓN EXITOSA] Deuda saldada para usuario '{}'. Suspensión levantada. {} empresas reactivadas. Vencimiento: {}",
                        user.getEmail(), userCompanies.size(), user.getPlanExpirationDate());
            }
        }
    }

    private void handlePaymentPending(MpPaymentResponse payment, String statusDetail, String externalRef) {
        log.warn("[CHECKOUT PRO WEBHOOK] ⏳ [PAGO PENDIENTE] Pago #{} en proceso (status_detail='{}'). Suscripción se mantiene PENDING sin otorgar beneficios.",
                payment.id(), statusDetail);
        // Zero-Trust: NO se otorgan beneficios ni se activa el plan.
    }

    private void handlePaymentRejected(MpPaymentResponse payment, String statusDetail, String externalRef) {
        log.warn("[CHECKOUT PRO WEBHOOK] ❌ [PAGO RECHAZADO] Pago #{} rechazado (status_detail='{}') | ExternalRef='{}'",
                payment.id(), statusDetail, externalRef);

        if (externalRef.startsWith("SUB:") || externalRef.startsWith("SUB_") || externalRef.startsWith("SUB-")
                || externalRef.startsWith("REC:") || externalRef.startsWith("REC_") || externalRef.startsWith("REC-")) {
            String[] parts = parseExternalReference(externalRef);
            if (parts.length >= 2) {
                String userId = parts[1];
                subscriptionRepository.findByUserId(userId).ifPresent(sub -> {
                    if (sub.getStatus() == SubscriptionStatus.PENDING) {
                        sub.setStatus(SubscriptionStatus.PAYMENT_FAILED);
                        subscriptionRepository.save(sub);
                        log.info("[CHECKOUT PRO WEBHOOK] ⚠️ Intento de suscripción/regularización ID: {} marcado como PAYMENT_FAILED.", sub.getId());
                    }
                });
            }
        } else if (externalRef.startsWith("UPG:") || externalRef.startsWith("UPG_") || externalRef.startsWith("UPG-")) {
            log.info("[CHECKOUT PRO WEBHOOK] ℹ️ Upgrade rechazado. El plan actual del usuario se mantiene intacto.");
        }
    }

    private void handlePaymentCancelled(MpPaymentResponse payment, String statusDetail, String externalRef) {
        log.warn("[CHECKOUT PRO WEBHOOK] 🚫 [PAGO CANCELADO] Pago #{} cancelado (status_detail='{}') | ExternalRef='{}'",
                payment.id(), statusDetail, externalRef);

        if (externalRef.startsWith("SUB:") || externalRef.startsWith("SUB_") || externalRef.startsWith("SUB-")
                || externalRef.startsWith("REC:") || externalRef.startsWith("REC_") || externalRef.startsWith("REC-")) {
            String[] parts = parseExternalReference(externalRef);
            if (parts.length >= 2) {
                String userId = parts[1];
                subscriptionRepository.findByUserId(userId).ifPresent(sub -> {
                    if (sub.getStatus() == SubscriptionStatus.PENDING) {
                        sub.setStatus(SubscriptionStatus.PAYMENT_FAILED);
                        subscriptionRepository.save(sub);
                        log.info("[CHECKOUT PRO WEBHOOK] ⚠️ Suscripción/regularización ID: {} marcada como PAYMENT_FAILED por cancelación.", sub.getId());
                    }
                });
            }
        }
    }

    private void handlePaymentRevoked(MpPaymentResponse payment, String statusDetail, String externalRef, boolean isChargedBack) {
        String status = payment.status() != null ? payment.status().toLowerCase() : "";
        log.warn("[CHECKOUT PRO WEBHOOK] 🚫 [REVOCACIÓN DE ACCESO] Pago #{} revocado con estado '{}' (detail: '{}', isChargedBack={}). Procesando deducción de días y políticas anti-contracargo...",
                payment.id(), status, statusDetail, isChargedBack);

        subscriptionRepository.findByExternalSubscriptionId(payment.id().toString())
                .or(() -> subscriptionRepository.findByExternalSubscriptionId(externalRef))
                .ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);

            User user = sub.getUser();
            if (user != null) {
                BillingPeriod period = sub.getBillingPeriod();
                int daysToDeduct = period != null ? period.getDays() : 30;
                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                // 1. Descuento proporcional de días del período desconocido
                if (user.getPlanExpirationDate() != null) {
                    OffsetDateTime currentExp = user.getPlanExpirationDate();
                    OffsetDateTime newExp = currentExp.minusDays(daysToDeduct);
                    user.setPlanExpirationDate(newExp);

                    if (!newExp.isAfter(now)) {
                        user.setCurrentPlan(SubscriptionPlan.NONE);
                    }
                } else {
                    user.setCurrentPlan(SubscriptionPlan.NONE);
                }

                // 2. Si es contracargo (charged_back): acumular deuda monetaria y suspender comercios (Soft Ban)
                if (isChargedBack) {
                    BigDecimal chargedBackAmount = payment.transactionAmount() != null ? payment.transactionAmount() : BigDecimal.ZERO;
                    BigDecimal currentDebt = user.getPendingDebtArs() != null ? user.getPendingDebtArs() : BigDecimal.ZERO;
                    user.setPendingDebtArs(currentDebt.add(chargedBackAmount));
                    user.setIsSuspendedForChargeback(true);

                    // Congelar todas las sucursales del usuario
                    List<Company> userCompanies = companyRepository.findAllByAdminId(user.getId());
                    userCompanies.forEach(c -> {
                        c.setIsEnabled(false);
                        c.setDisabledDate(now);
                    });
                    companyRepository.saveAll(userCompanies);

                    log.warn("[CHECKOUT PRO WEBHOOK] 🚨 [CONTRACARGO DETECTADO] Usuario '{}' suspendido. Deuda acumulada: {} ARS (+{} ARS). Vencimiento ajustado: {}. {} sucursales congeladas.",
                            user.getEmail(), user.getPendingDebtArs(), chargedBackAmount, user.getPlanExpirationDate(), userCompanies.size());
                } else {
                    log.info("[CHECKOUT PRO WEBHOOK] ℹ️ [REEMBOLSO] Reembolso aplicado para usuario '{}'. Días descontados: {}. Nuevo vencimiento: {}",
                            user.getEmail(), daysToDeduct, user.getPlanExpirationDate());
                }

                userRepository.save(user);
            }
        });
    }


    private String[] parseExternalReference(String externalRef) {
        if (!StringUtils.hasText(externalRef)) {
            return new String[0];
        }
        if (externalRef.contains(":")) {
            return externalRef.split(":");
        }
        if (externalRef.contains("_")) {
            return externalRef.split("_");
        }
        return externalRef.split("-");
    }

    private boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String extractTopic(Map<String, Object> payload) {
        if (payload == null) return "payment";
        Object topic = payload.get("topic");
        if (topic != null) return topic.toString().toLowerCase();
        Object type = payload.get("type");
        if (type != null) return type.toString().toLowerCase();
        Object resource = payload.get("resource");
        if (resource != null && resource.toString().toLowerCase().contains("merchant_orders")) return "merchant_order";
        return "payment";
    }

    @SuppressWarnings("unchecked")
    private String extractEntityId(Map<String, Object> payload) {
        if (payload == null) return null;

        // 1. data: { id: "..." }
        Object dataObj = payload.get("data");
        if (dataObj instanceof Map<?, ?> dataMap) {
            Object idObj = dataMap.get("id");
            if (idObj != null) return idObj.toString();
        }

        // 2. data.id directo
        Object dotId = payload.get("data.id");
        if (dotId != null) return dotId.toString();

        // 3. Campo id
        Object id = payload.get("id");
        if (id != null) return id.toString();

        // 4. resource URL: .../payments/12345 o .../merchant_orders/12345
        Object resource = payload.get("resource");
        if (resource != null) {
            String resStr = resource.toString();
            int lastSlash = resStr.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < resStr.length() - 1) {
                return resStr.substring(lastSlash + 1);
            }
        }

        return null;
    }
}


