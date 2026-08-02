package com.tech.point_system.config;

import com.tech.point_system.model.User;
import com.tech.point_system.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SupabaseJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public SupabaseJwtConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String supabaseUserId = jwt.getSubject();
        log.debug("Procesando token para el usuario de Supabase ID: {}", supabaseUserId);

        User user = userRepository.findById(supabaseUserId)
                .orElseThrow(() -> {
                    log.error("Usuario con ID {} no encontrado en la base de datos local", supabaseUserId);
                    return new RuntimeException("Usuario no encontrado");
                });

        String roleName = "ROLE_" + user.getRole().name();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

        log.info("Usuario autenticado exitosamente con rol: {}", roleName);

        return new JwtAuthenticationToken(jwt, authorities);
    }
}