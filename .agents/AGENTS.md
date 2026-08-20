# Project Behavioral Rules & Guidelines

## Responsive Design Requirement (Mandatory)
- Every UI component, layout, page, modal, table, and form added or modified must be 100% responsive across all screen sizes (mobile `<640px`, tablet `640px-1024px`, and desktop `>1024px`).
- Use modern responsive design best practices:
  - Mobile-first or responsive-first layout principles using Tailwind CSS utilities (`sm:`, `md:`, `lg:`, `xl:`).
  - Touch-friendly tap targets (`min-h-[44px]`, comfortable padding, accessible buttons).
  - Collapsible mobile navigation drawers/menus for headers and navigation bars.
  - Horizontal scrolling containers, card grids, or stacked layouts for data tables on small screens.
  - Responsive typography, image aspect ratios (`object-contain` / `object-cover`), and dynamic modal sizing (`w-full max-w-lg px-4`).

## Template Syntax & Verification Integrity (Mandatory)
- **Zero Syntax Errors**: Always verify closing tags and class binding syntax in Angular HTML templates (e.g. use ternary expressions `[class]="..."` instead of invalid syntax like `[class.bg-[#HEX]="..."`).
- **Empirical Build Verification**: Never claim a task or refactor is completed without verifying with `ng build` or runtime checks to ensure 0 compilation errors.
- **Production-Ready UI**: Never include developer debug text, API endpoints (`GET /api/...`), or raw backend tech tags in user-facing UI components.

## Anti-AI Cliché & Minimalist UI Rules (Mandatory)
- **No AI Cliché Eyebrow Pills/Chips**: Do NOT add pill-shaped badge capsules with glowing/pulsing dots (e.g. `🟢 Panel General de Usuario`, `🟢 Planes & Precios Flexibles`, `🟢 Portal Público para Clientes`, `uppercase tracking-widest text-xs rounded-full`) stacked on top of main headings (`<h1>`, `<h2>`).
- **Minimalist & Human Design**: Keep page headers, dashboard titles, and banners clean, direct, and uncluttered. Use natural typographical hierarchy (clear heading, concise subtitle, direct actions) instead of redundant decorative tags and pseudo-status capsules.
- **Functional Status Indicators Only**: Badges must only be used when they represent actual functional status (e.g., `Habilitada` / `Deshabilitada`, `Activo`, `Vencido`), and should be styled with clean, subtle, and minimalist tokens without exaggerated animations or decorative dots.

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
  - Toda anotación `@Cacheable` DEBE venir acompañada obligatoriamente de su estrategia de invalidación precisa con `@CacheEvict` (o `@Caching(evict = ...)`) en los métodos de mutación (POST, PUT, DELETE, activación/desactivación).

