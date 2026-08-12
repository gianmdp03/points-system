package com.tech.point_system.service.impl;

import com.tech.point_system._enum.AppAdminOwner;
import com.tech.point_system._enum.Role;
import com.tech.point_system._enum.TransactionType;
import com.tech.point_system.extra.CompanyDetails;
import com.tech.point_system.model.*;
import com.tech.point_system.repository.*;
import com.tech.point_system.service.DataSeederService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSeederServiceImpl implements DataSeederService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final PromotionRepository promotionRepository;
    private final RewardRepository rewardRepository;
    private final PointsAccountRepository pointsAccountRepository;
    private final SaleRepository saleRepository;
    private final PointsTransactionRepository transactionRepository;

    @Override
    @Transactional
    public void seedData() {
        log.info("Iniciando la inspección de usuarios y carga de datos de prueba...");

        List<User> existingUsers = userRepository.findAll();
        if (existingUsers.isEmpty()) {
            throw new IllegalStateException("No hay usuarios en la BD. Registrá/sincronizá los usuarios desde Supabase primero.");
        }

        // 1. Filtrar los usuarios según su rol real asignado
        User appAdmin = existingUsers.stream()
                .filter(u -> u.getRole() == Role.APP_ADMIN)
                .findFirst()
                .orElse(null);

        User companyAdmin = existingUsers.stream()
                .filter(u -> u.getRole() == Role.COMPANY_ADMIN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se encontró ningún usuario con rol COMPANY_ADMIN en la base de datos."));

        // CREACIÓN DE CLIENTES REALES (Nueva Entidad Client)
        Client client1 = clientRepository.getOrCreateClient("11111111", "Argentina", "Juan Perez", "juan@test.com", "123456789");
        Client client2 = clientRepository.getOrCreateClient("22222222", "Argentina", "Maria Gomez", "maria@test.com", "987654321");

        // Inicializar prueba gratuita para el Company Admin si no la tiene configurada
        if (companyAdmin.getIsFreeTrialOver() == null) {
            LocalDate today = LocalDate.now();
            companyAdmin.setIsFreeTrialOver(false);
            companyAdmin.setFreeTrialStartTime(today);
            companyAdmin.setFreeTrialEndTime(today.plusDays(30));
            userRepository.save(companyAdmin);
        }

        // Logs descriptivos para validar en consola la asignación
        log.info("Clasificación de usuarios detectada:");
        log.info(" -> APP_ADMIN: {}", appAdmin != null ? appAdmin.getEmail() : "Sin asignar en BD");
        log.info(" -> COMPANY_ADMIN: {} ({})", companyAdmin.getEmail(), companyAdmin.getId());
        log.info(" -> CLIENTE 1: {} ({})", client1.getEmail(), client1.getDni());
        log.info(" -> CLIENTE 2: {} ({})", client2.getEmail(), client2.getDni());

        // 2. Crear Empresas administradas por el COMPANY_ADMIN real
        CompanyDetails details1 = new CompanyDetails("Argentina", "Buenos Aires", "CABA", "Av. Corrientes 1234", "1043");
        Company company1 = new Company();
        company1.setName("Coffee Station");
        company1.setCompanyDetails(details1);
        company1.setAdmin(companyAdmin);
        company1.setAmountStep(new BigDecimal("100.00")); // Cada $100 -> 10 pts
        company1.setPointsPerStep(10);
        company1.setAppAdminOwner(AppAdminOwner.GIANLUCA);
        company1.setIsEnabled(true);
        company1 = companyRepository.save(company1);

        CompanyDetails details2 = new CompanyDetails("Argentina", "Buenos Aires", "Mar del Plata", "Güemes 2500", "7600");
        Company company2 = new Company();
        company2.setName("Tech Store MDP");
        company2.setCompanyDetails(details2);
        company2.setAdmin(companyAdmin);
        company2.setAmountStep(new BigDecimal("500.00")); // Cada $500 -> 25 pts
        company2.setPointsPerStep(25);
        company2.setAppAdminOwner(AppAdminOwner.ORGANIC);
        company2.setIsEnabled(true);
        company2 = companyRepository.save(company2);

        // 3. Crear Productos
        Product p1 = new Product();
        p1.setName("Café Espresso Doble");
        p1.setDescription("Café tostado artesanal de especialidad.");
        p1.setPrice(new BigDecimal("1200.00"));
        p1.setImage("https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd");
        p1.setCompany(company1);

        Product p2 = new Product();
        p2.setName("Medialuna de Manteca");
        p2.setDescription("Horneadas en el día.");
        p2.setPrice(new BigDecimal("450.00"));
        p2.setImage("https://images.unsplash.com/photo-1555507036-ab1f4038808a");
        p2.setCompany(company1);

        Product p3 = new Product();
        p3.setName("Auriculares Bluetooth Pro");
        p3.setDescription("Cancelación activa de ruido.");
        p3.setPrice(new BigDecimal("35000.00"));
        p3.setImage("https://images.unsplash.com/photo-1505740420928-5e560c06d30e");
        p3.setCompany(company2);

        productRepository.saveAll(List.of(p1, p2, p3));

        // 4. Promociones
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Promotion promo1 = new Promotion();
        promo1.setName("Semana del Café 2x Puntos");
        promo1.setDescription("¡Duplica tus puntos en todas las compras esta semana!");
        promo1.setStartDate(now.minusDays(2));
        promo1.setEndDate(now.plusDays(5));
        promo1.setMultiplier(new BigDecimal("2.0"));
        promo1.setIsEnabled(true);
        promo1.setCompany(company1);
        promotionRepository.save(promo1);

        // 5. Premios (Rewards)
        Reward r1 = new Reward();
        r1.setName("Café Gratis a Elección");
        r1.setDescription("Canjeable por cualquier bebida caliente de la carta.");
        r1.setCostInPoints(150);
        r1.setIsEnabled(true);
        r1.setCompany(company1);

        Reward r2 = new Reward();
        r2.setName("Voucher de Descuento $5000");
        r2.setDescription("Válido en compras superiores a $20000.");
        r2.setCostInPoints(500);
        r2.setIsEnabled(true);
        r2.setCompany(company2);

        rewardRepository.saveAll(List.of(r1, r2));

        // 6. Cuentas de Puntos asignadas a los clientes
        PointsAccount account1 = new PointsAccount();
        account1.setClient(client1);
        account1.setCompany(company1);
        account1.setBalance(350);
        account1 = pointsAccountRepository.save(account1);

        PointsAccount account2 = new PointsAccount();
        account2.setClient(client2);
        account2.setCompany(company1);
        account2.setBalance(120);
        account2 = pointsAccountRepository.save(account2);

        PointsAccount account3 = new PointsAccount();
        account3.setClient(client1);
        account3.setCompany(company2);
        account3.setBalance(600);
        account3 = pointsAccountRepository.save(account3);

        // 7. Registro de Ventas a Clientes
        Sale sale1 = new Sale();
        sale1.setClient(client1);
        sale1.setCompany(company1);
        sale1.setAmount(new BigDecimal("2500.00"));

        Sale sale2 = new Sale();
        sale2.setClient(client2);
        sale2.setCompany(company1);
        sale2.setAmount(new BigDecimal("1200.00"));

        saleRepository.saveAll(List.of(sale1, sale2));

        // 8. Historial de Transacciones de Puntos
        PointsTransaction t1 = new PointsTransaction();
        t1.setPointsAccount(account1);
        t1.setAmount(500);
        t1.setTransactionType(TransactionType.EARNED);
        t1.setCreatedAt(now.minusDays(3));

        PointsTransaction t2 = new PointsTransaction();
        t2.setPointsAccount(account1);
        t2.setAmount(-150);
        t2.setTransactionType(TransactionType.REDEEMED);
        t2.setCreatedAt(now.minusDays(1));

        transactionRepository.saveAll(List.of(t1, t2));

        log.info("¡Estructura de datos creada exitosamente vinculada a la nueva entidad Client!");
    }
}