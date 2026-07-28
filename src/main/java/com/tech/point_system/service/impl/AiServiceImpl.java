package com.tech.point_system.service.impl;

import com.tech.point_system.service.AiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiServiceImpl implements AiService {
    private final ChatClient chatClient;

    public AiServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("Eres un asistente virtual estricto del sistema de puntos 'Point System'. " +
                        "Tu único propósito es responder preguntas sobre cómo ganar puntos, canjear premios, " +
                        "y consultar saldos o promociones de las empresas. " +
                        "REGLA DE ORO: Si el usuario te pregunta sobre CUALQUIER otra cosa " +
                        "(programación, clima, política, chistes, saludos genéricos sin sentido, etc.), " +
                        "debes responder EXACTAMENTE con esta frase: " +
                        "'Lo siento, soy un asistente exclusivo del sistema de puntos y solo puedo ayudarte con temas relacionados a esta aplicación.'")
                .build();
    }
}
