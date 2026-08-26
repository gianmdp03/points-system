package com.tech.point_system.payment;

import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system.payment.impl.MercadoPagoPaymentStrategy;
import com.tech.point_system.payment.impl.MockPaymentStrategy;
import com.tech.point_system.payment.impl.PaddlePaymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentStrategyFactoryTest {

    @Mock
    private MockPaymentStrategy mockPaymentStrategy;

    @Mock
    private MercadoPagoPaymentStrategy mercadoPagoPaymentStrategy;

    @Mock
    private PaddlePaymentStrategy paddlePaymentStrategy;

    private PaymentStrategyFactory factory;

    @BeforeEach
    void setUp() {
        when(mockPaymentStrategy.getProvider()).thenReturn(PaymentProvider.MOCK);
        when(mercadoPagoPaymentStrategy.getProvider()).thenReturn(PaymentProvider.MERCADO_PAGO);
        when(paddlePaymentStrategy.getProvider()).thenReturn(PaymentProvider.PADDLE);

        factory = new PaymentStrategyFactory(List.of(mockPaymentStrategy, mercadoPagoPaymentStrategy, paddlePaymentStrategy));
    }

    @Test
    void testGetStrategy_MercadoPago() {
        PaymentStrategy strategy = factory.getStrategy(PaymentProvider.MERCADO_PAGO);
        assertNotNull(strategy);
        assertEquals(PaymentProvider.MERCADO_PAGO, strategy.getProvider());
    }

    @Test
    void testGetStrategy_Mock() {
        PaymentStrategy strategy = factory.getStrategy(PaymentProvider.MOCK);
        assertNotNull(strategy);
        assertEquals(PaymentProvider.MOCK, strategy.getProvider());
    }
}
