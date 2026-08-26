package com.tech.point_system.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mercadopago")
public class MercadoPagoProperties {

    private String accessToken;
    private String publicKey;
    private String webhookSecret;
    private String baseUrl = "https://api.mercadopago.com";
    private String backUrl = "http://localhost:4200/subscription/callback";
    private boolean sandbox = true;


    @PostConstruct
    public void init() {
        if (StringUtils.hasText(accessToken)) {
            MercadoPagoConfig.setAccessToken(accessToken);
            log.info("[MERCADO PAGO] SDK inicializado correctamente con access token configurado.");
        } else {
            log.warn("[MERCADO PAGO] Access token no configurado. La pasarela Mercado Pago operará en modo degradado.");
        }
    }
}
