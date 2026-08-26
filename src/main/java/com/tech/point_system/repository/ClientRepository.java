package com.tech.point_system.repository;

import com.tech.point_system.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByDniAndCountry(String dni, String country);

    default Client getOrCreateClient(String dni, String country, String name, String email, String phone) {
        return getOrCreateClient(dni, country, name, email, phone, true);
    }

    default Client getOrCreateClient(String dni, String country, String name, String email, String phone, Boolean isNotificationEnabled) {
        return findByDniAndCountry(dni, country).map(existing -> {
            if (isNotificationEnabled != null && !isNotificationEnabled.equals(existing.getIsNotificationEnabled())) {
                existing.setIsNotificationEnabled(isNotificationEnabled);
                return save(existing);
            }
            return existing;
        }).orElseGet(() -> {
            Client newClient = new Client();
            newClient.setDni(dni != null ? dni.trim() : "");
            newClient.setCountry(country != null ? country.trim() : "Argentina");
            newClient.setName(name != null ? name.trim() : "");
            newClient.setEmail(email != null && !email.isBlank() ? email.trim() : null);
            newClient.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
            newClient.setIsNotificationEnabled(isNotificationEnabled != null ? isNotificationEnabled : true);
            return save(newClient);
        });
    }
}
