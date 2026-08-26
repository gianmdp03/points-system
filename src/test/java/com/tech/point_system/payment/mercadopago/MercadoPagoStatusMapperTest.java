package com.tech.point_system.payment.mercadopago;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercadoPagoStatusMapperTest {

    @Test
    void testApprovedStatus() {
        String msg = MercadoPagoStatusMapper.getUserFriendlyMessage("approved", "accredited");
        assertEquals("¡Pago aprobado con éxito!", msg);
    }

    @Test
    void testPendingStatus() {
        String msg1 = MercadoPagoStatusMapper.getUserFriendlyMessage("in_process", "pending_contingency");
        assertEquals("Estamos procesando tu pago. En breve te confirmaremos la acreditación.", msg1);

        String msg2 = MercadoPagoStatusMapper.getUserFriendlyMessage("pending", "pending_review_manual");
        assertEquals("Tu pago se encuentra en revisión de seguridad. Te avisaremos cuando sea aprobado.", msg2);
    }

    @Test
    void testRejectedStatuses() {
        assertEquals("Fondos insuficientes en la tarjeta seleccionada.",
                MercadoPagoStatusMapper.getUserFriendlyMessage("rejected", "cc_rejected_insufficient_amount"));

        assertEquals("Debes autorizar el pago con tu entidad emisora antes de continuar.",
                MercadoPagoStatusMapper.getUserFriendlyMessage("rejected", "cc_rejected_call_for_authorize"));

        assertEquals("El código de seguridad (CVV) ingresado es inválido.",
                MercadoPagoStatusMapper.getUserFriendlyMessage("rejected", "cc_rejected_bad_filled_security_code"));

        assertEquals("La fecha de vencimiento de la tarjeta es incorrecta.",
                MercadoPagoStatusMapper.getUserFriendlyMessage("rejected", "cc_rejected_bad_filled_date"));

        assertEquals("Verifica los datos del formulario de pago; hay campos incorrectos.",
                MercadoPagoStatusMapper.getUserFriendlyMessage("rejected", "cc_rejected_bad_filled_other"));

        assertEquals("El número de tarjeta ingresado es inválido.",
                MercadoPagoStatusMapper.getUserFriendlyMessage("rejected", "cc_rejected_bad_filled_card_number"));

        assertEquals("Tu tarjeta se encuentra inhabilitada. Comunícate con tu banco emisor.",
                MercadoPagoStatusMapper.getUserFriendlyMessage("rejected", "cc_rejected_card_disabled"));

        assertEquals("El pago fue rechazado por políticas de prevención de fraude.",
                MercadoPagoStatusMapper.getUserFriendlyMessage("rejected", "cc_rejected_high_risk"));
    }

    @Test
    void testUnknownOrFallbackStatus() {
        String msg = MercadoPagoStatusMapper.getUserFriendlyMessage("rejected", "unknown_custom_detail");
        assertTrue(msg.contains("Pago rechazado"));
    }
}
