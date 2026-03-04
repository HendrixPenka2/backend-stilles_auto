package com.Team_Pk.car_rental.catalog.listener;

import com.Team_Pk.car_rental.catalog.config.RabbitMQConfig;
import com.Team_Pk.car_rental.catalog.dto.ReviewEvent;
import com.Team_Pk.car_rental.catalog.service.CommunityWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 🎧 Listener RabbitMQ pour Catalog Service
 * 
 * Écoute les événements de Community Service pour :
 * - Invalider le cache des notations quand un avis est approuvé/rejeté
 * - Rafraîchir les données affichées aux clients
 * 
 * Architecture Event-Driven : Les services communiquent via RabbitMQ
 * sans dépendances directes (découplage)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventListener {

    private final CommunityWebClient communityWebClient;

    /**
     * 🎯 Listener : Avis approuvé
     * 
     * Action : Invalide le cache pour que la prochaine requête récupère la vraie note mise à jour
     * 
     * Scénario :
     * 1. Admin approuve un avis 5⭐ dans community-service
     * 2. Community-service envoie l'événement "review.approved"
     * 3. Catalog-service reçoit l'événement ICI
     * 4. On invalide le cache pour {entityType, entityId}
     * 5. La prochaine fois qu'un client affiche le véhicule → nouvelle note visible
     */
    @RabbitListener(queues = RabbitMQConfig.REVIEW_APPROVED_QUEUE)
    public void handleReviewApproved(ReviewEvent event) {
        try {
            log.info("📥 Event reçu: review.approved pour {}:{} (Note: {}⭐)", 
                     event.getEntityType(), event.getEntityId(), event.getRating());
            
            // Invalide le cache dans CommunityWebClient
            communityWebClient.invalidateCache(event.getEntityType(), event.getEntityId());
            
            log.info("✅ Cache invalidé avec succès pour {}:{}", 
                     event.getEntityType(), event.getEntityId());
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de review.approved", e);
        }
    }

    /**
     * 🎯 Listener : Avis rejeté
     * 
     * Action : Optionnel - pour les logs ou statistiques
     * 
     * Note : Le rejet d'un avis ne change PAS la note moyenne (l'avis n'était pas compté),
     * donc pas besoin d'invalider le cache. On log juste pour le monitoring.
     */
    @RabbitListener(queues = RabbitMQConfig.REVIEW_REJECTED_QUEUE)
    public void handleReviewRejected(ReviewEvent event) {
        try {
            log.info("📥 Event reçu: review.rejected pour {}:{} (Avis refusé)", 
                     event.getEntityType(), event.getEntityId());
            
            // Optionnel : Vous pourriez collecter ces stats pour un dashboard admin
            // Par exemple : combien d'avis refusés par véhicule
            
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de review.rejected", e);
        }
    }
}
