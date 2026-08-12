package com.tech.point_system.repository;

import com.tech.point_system.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByDniAndCountry(String dni, String country);

    default Client getOrCreateClient(String dni, String country, String name, String email, String phone) {
        return findByDniAndCountry(dni, country).orElseGet(() -> {
            Client newClient = new Client();
            newClient.setDni(dni);
            newClient.setCountry(country);
            newClient.setName(name);
            newClient.setEmail(email);
            newClient.setPhone(phone);
            return save(newClient);
        });
    }
}
