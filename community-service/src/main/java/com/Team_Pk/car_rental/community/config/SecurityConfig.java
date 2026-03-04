package com.Team_Pk.car_rental.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
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

import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
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
                        
                        // 2. Avis Publics (Lecture seule des avis approuvés)
                        // Attention : On spécifie GET pour ne pas ouvrir le POST (création) à tout le monde
                        .pathMatchers(HttpMethod.GET, "/api/v1/reviews").permitAll()
                        
                        // 3. Ratings (Public - utilisé par catalog-service pour afficher les notes)
                        .pathMatchers(HttpMethod.GET, "/api/v1/ratings/**").permitAll()
                        // Note : /api/v1/reviews/my reste protégé par .authenticated() plus bas
                        
                        // 4. Admin (Modération des avis)
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        
                        // 5. Tout le reste (Créer un avis, Wishlist, Voir ses avis) nécessite d'être connecté
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
     * Décodage du Token - SYMÉTRIQUE AVEC AUTH & CATALOG
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // On crée la clé secrète à partir des octets de la chaîne texte
        SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }
}
