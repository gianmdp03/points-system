package com.tech.point_system.payment;

import com.tech.point_system._enum.PaymentProvider;
import com.tech.point_system.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyFactory {

    private final Map<PaymentProvider, PaymentStrategy> strategies;

    public PaymentStrategyFactory(List<PaymentStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(PaymentStrategy::getProvider, Function.identity()));
    }

    public PaymentStrategy getStrategy(PaymentProvider provider) {
        PaymentStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new BadRequestException("Proveedor de pago no soportado: " + provider);
        }
        return strategy;
    }
}