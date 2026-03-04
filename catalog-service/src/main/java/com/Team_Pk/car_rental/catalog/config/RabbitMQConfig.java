package com.Team_Pk.car_rental.catalog.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration RabbitMQ pour Catalog Service
 * 
 * Catalog écoute les événements de Community Service :
 * - review.approved : Pour invalider le cache des notes
 * - review.rejected : Pour logger les rejets (optionnel)
 */
@Configuration
public class RabbitMQConfig {

    // Exchange commun à tous les services (Topic Exchange pour le routing)
    public static final String EXCHANGE_NAME = "stilles_auto_exchange";

    // Queues dédiées à catalog-service
    public static final String REVIEW_APPROVED_QUEUE = "catalog.review.approved";
    public static final String REVIEW_REJECTED_QUEUE = "catalog.review.rejected";

    // Routing keys (doivent correspondre à celles de community-service)
    public static final String REVIEW_APPROVED_KEY = "review.approved";
    public static final String REVIEW_REJECTED_KEY = "review.rejected";

    /**
     * Déclaration de l'exchange (partagé avec community-service)
     */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * Queue pour les avis approuvés
     */
    @Bean
    public Queue reviewApprovedQueue() {
        return new Queue(REVIEW_APPROVED_QUEUE, true); // durable = true
    }

    /**
     * Queue pour les avis rejetés (optionnel, pour logs)
     */
    @Bean
    public Queue reviewRejectedQueue() {
        return new Queue(REVIEW_REJECTED_QUEUE, true);
    }

    /**
     * Binding : review.approved → catalog.review.approved
     */
    @Bean
    public Binding reviewApprovedBinding(@Qualifier("reviewApprovedQueue") Queue reviewApprovedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(reviewApprovedQueue)
                .to(exchange)
                .with(REVIEW_APPROVED_KEY);
    }

    /**
     * Binding : review.rejected → catalog.review.rejected
     */
    @Bean
    public Binding reviewRejectedBinding(@Qualifier("reviewRejectedQueue") Queue reviewRejectedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(reviewRejectedQueue)
                .to(exchange)
                .with(REVIEW_REJECTED_KEY);
    }

    /**
     * Convertisseur JSON pour désérialiser les événements
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
