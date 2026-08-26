package com.tech.point_system.payment.mercadopago;

import org.springframework.util.StringUtils;

public class MercadoPagoStatusMapper {

    public static String getUserFriendlyMessage(String status, String statusDetail) {
        if (!StringUtils.hasText(status)) {
            return "No fue posible procesar el pago debido a un error general de la entidad.";
        }

        String normalizedStatus = status.trim().toLowerCase();
        String detail = StringUtils.hasText(statusDetail) ? statusDetail.trim().toLowerCase() : "";

        return switch (normalizedStatus) {
            case "approved", "processed", "accredited" -> "¡Pago aprobado con éxito!";
            
            case "in_process", "pending", "action_required", "payment_required" -> switch (detail) {
                case "pending_contingency" -> "Estamos procesando tu pago. En breve te confirmaremos la acreditación.";
                case "pending_review_manual" -> "Tu pago se encuentra en revisión de seguridad. Te avisaremos cuando sea aprobado.";
                case "pending_waiting_payment", "pending_waiting_transfer" -> "Esperando acreditación del pago.";
                default -> "El pago se encuentra en proceso de validación.";
            };
            
            case "rejected", "cancelled", "failed" -> switch (detail) {
                case "cc_rejected_insufficient_amount", "fund" -> "Fondos insuficientes en la tarjeta seleccionada.";
                case "cc_rejected_call_for_authorize", "call" -> "Debes autorizar el pago con tu entidad emisora antes de continuar.";
                case "cc_rejected_bad_filled_security_code", "secu" -> "El código de seguridad (CVV) ingresado es inválido.";
                case "cc_rejected_bad_filled_date", "expi" -> "La fecha de vencimiento de la tarjeta es incorrecta.";
                case "cc_rejected_bad_filled_other", "form" -> "Verifica los datos del formulario de pago; hay campos incorrectos.";
                case "cc_rejected_bad_filled_card_number" -> "El número de tarjeta ingresado es inválido.";
                case "cc_rejected_card_disabled" -> "Tu tarjeta se encuentra inhabilitada. Comunícate con tu banco emisor.";
                case "cc_rejected_card_error" -> "No pudimos procesar tu tarjeta. Por favor, intenta con otro medio de pago.";
                case "cc_rejected_duplicated_payment" -> "Ya se registró un pago idéntico recientemente. Verifica tu cuenta.";
                case "cc_rejected_high_risk" -> "El pago fue rechazado por políticas de prevención de fraude.";
                case "cc_rejected_max_attempts" -> "Superaste el límite máximo de intentos permitidos con esta tarjeta.";
                case "cc_rejected_invalid_installments" -> "La tarjeta no permite la cantidad de cuotas seleccionadas.";
                case "cc_rejected_other_reason", "othe" -> "No fue posible procesar el pago debido a un error general de la entidad.";
                default -> StringUtils.hasText(statusDetail)
                        ? "Pago rechazado (" + statusDetail + "). Por favor, intenta con otro medio de pago."
                        : "No fue posible procesar el pago debido a un error general de la entidad.";
            };
            
            default -> "Estado de pago desconocido (" + status + "). Por favor verifica tu cuenta.";
        };
    }
}
