---
name: mercadopago-integration
description: Guía y especificación oficial para integrar pagos, webhooks y APIs de Mercado Pago en Spring Boot 4 y Java 25. Usar siempre que se creen DTOs, clientes HTTP o controladores vinculados a Mercado Pago.
---

# Mercado Pago Integration Skill

## Contexto y Referencia

- Documentación general: Consulta `resources/mp_docs.txt` para flujos de checkout, estados de pago, ciclo de vida de transacciones y autenticación.
- Especificación de API: Consulta `resources/mp_api_reference.txt` para validar endpoints exactos, payloads JSON, query params y códigos de respuesta.

## Reglas de Implementación en Spring Boot 4 & Java 25

1. **Modelado y Tipado (Java 25):**
   - Usa `record` inmutables para todos los DTOs de Request, Response y Webhooks.
   - Aplica Pattern Matching en `switch` exhaustivos para el manejo de estados de pago (`approved`, `pending`, `rejected`, etc.).
2. **Cliente HTTP:**
   - Usa la infraestructura nativa de `RestClient` o HTTP Interfaces declarativas (`@HttpExchange`) configuradas para Spring Boot 4.
3. **Concurrencia y Rendimiento:**
   - Diseña el consumo y despacho de eventos con Virtual Threads habilitados por defecto.
4. **Seguridad y Webhooks:**
   - Valida la firma `x-signature` (HMAC-SHA256) siguiendo las pautas de `resources/mp_docs.txt` antes de despachar el procesamiento asíncrono del evento.
