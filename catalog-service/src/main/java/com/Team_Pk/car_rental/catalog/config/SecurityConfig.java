package com.Team_Pk.car_rental.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // 1. Swagger (Public)
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        
                        // 2. Catalogue (Lecture Publique)
                        .pathMatchers("/api/v1/vehicles/**").permitAll()
                        .pathMatchers("/api/v1/accessories/**").permitAll()
                        
                        // 3. Admin (Modification)
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        
                        // 4. Tout le reste authentifié
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
                )
                .build();
    }

    /**
     * Extraction des rôles depuis le JWT
     */
    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        return jwt -> {
            String role = jwt.getClaimAsString("role");
            Collection<GrantedAuthority> authorities = (role != null) ? 
                List.of(new SimpleGrantedAuthority("ROLE_" + role)) : 
                Collections.emptyList();
                
            return Mono.just(new JwtAuthenticationToken(jwt, authorities));
        };
    }

    /**
     * Décodage du Token - SYMÉTRIQUE AVEC AUTH-SERVICE
     */
    /**
     * MODIFICATION ICI : Utilisation de getBytes() au lieu de Base64.getDecoder()
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // On crée la clé secrète à partir des octets de la chaîne texte
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }
}