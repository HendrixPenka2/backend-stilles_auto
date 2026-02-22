package com.Team_Pk.car_rental.catalog.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;

@Configuration
@EnableR2dbcAuditing // Permet de remplir @CreatedDate et @LastModifiedDate automatiquement
public class R2dbcConfig {

    // On dit à R2DBC de comprendre nos ENUMS personnalisés de PostgreSQL
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        
        // C'est facultatif si on utilise que des VARCHAR, mais obligatoire pour les types ENUM Postgres natifs
        // Pour faire simple avec R2DBC, on laisse souvent Spring Data faire le mapping automatique.
        return initializer;
    }
}
