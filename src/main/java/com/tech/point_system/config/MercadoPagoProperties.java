package com.tech.point_system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mercadopago")
public class MercadoPagoProperties {

    private String accessToken = "";
    private String publicKey = "";
    private String webhookSecret = "";
    private String baseUrl = "https://api.mercadopago.com";
    private String backUrl = "http://localhost:3000/subscription/callback";
}
