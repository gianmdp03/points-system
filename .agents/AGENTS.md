# Project Behavioral Rules & Guidelines

## Backend Strict Optimization Rules (Mandatory)

- **CAPA 1: Base de Datos y JPA**:
  - Erradicar consultas N+1 en todas las relaciones mediante `@EntityGraph`, `JOIN FETCH` o consultas con cláusula `IN`.
  - Usar DTO Projections para consultas de solo lectura para evitar instanciar entidades completas en el contexto de persistencia de Hibernate.
  - Asegurar la indexación adecuada de claves foráneas y columnas de filtrado frecuente (`clientId`, `companyId`, `createdAt`, `lastActivityDate`, etc.).
- **CAPA 2: Complejidad Algorítmica (Target O(1))**:
  - Eliminar bucles anidados $O(N^2)$ y `.stream().filter()` repetitivos en caliente, reemplazándolos con estructuras de datos eficientes (`HashMap`, `HashSet`) para indexación y lookup en memoria en $O(1)$.
- **CAPA 3: Concurrencia y Transaccionalidad**:
  - Separar operaciones no bloqueantes (listeners de eventos, auditorías secundarias, notificaciones) a ejecución asíncrona en background con `@Async`.
  - Aplicar `@Transactional(readOnly = true)` de forma explícita y estricta en todas las operaciones y servicios de solo lectura para optimizar dirty-checking en Hibernate y conexiones JDBC.
- **CAPA 4: Caché Inteligente**:
  - Identificar catálogos estáticos y datos de lectura pública (premios, empresas públicas, promociones).
  - **ADVERTENCIA ESTRICTA**: Bajo ningún punto de vista cachear saldos de clientes, cuentas de puntos, ventas ni transacciones. La consistencia financiera de puntos debe ser siempre 100% en tiempo real.
  - Toda anotación `@Cacheable` DEBE venir acompañada obligatoriamente de su estrategia de invalidación precisa con `@CacheEvict` (o `@Caching(evict = ...)`) en los métodos de mutación (POST, PUT, PATCH, DELETE, activación/desactivación).
