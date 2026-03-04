package com.Team_Pk.car_rental.auth.controller;

import com.Team_Pk.car_rental.auth.dto.ChangeRoleRequest;
import com.Team_Pk.car_rental.auth.dto.ChangeStatusRequest;
import com.Team_Pk.car_rental.auth.dto.JwtResponse;
import com.Team_Pk.car_rental.auth.dto.LoginRequest;
import com.Team_Pk.car_rental.auth.dto.RegisterRequest;
import com.Team_Pk.car_rental.auth.dto.ResetPasswordRequest;
import com.Team_Pk.car_rental.auth.dto.UpdateProfileRequest;
import com.Team_Pk.car_rental.auth.dto.UserResponse;
import com.Team_Pk.car_rental.auth.dto.VerificationRequest;
import com.Team_Pk.car_rental.auth.entity.User;
import com.Team_Pk.car_rental.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Gestion de l'inscription, connexion, vérification email et réinitialisation de mot de passe")
public class AuthController {

    private final AuthService authService;

    /**
     * ✍️ ENDPOINT PUBLIC - Inscription d'un nouvel utilisateur
     * Crée un compte avec le rôle CLIENT par défaut et le statut is_active=false
     * Envoie un email avec un code de vérification à 6 chiffres
     * L'utilisateur doit vérifier son email avant de pouvoir se connecter
     */
    @Operation(summary = "Inscription", 
               description = "Créer un nouveau compte utilisateur. Envoie un email de vérification.")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> register(
            @Parameter(description = "Données d'inscription : email, mot de passe, prénom, nom, téléphone")
            @Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * ✅ ENDPOINT PUBLIC - Vérification de l'email après inscription
     * Valide le code à 6 chiffres reçu par email
     * Active le compte (is_active=true) pour permettre la connexion
     * Le code expire après 24 heures
     */
    @Operation(summary = "Vérifier l'email", 
               description = "Valide l'adresse email avec le code reçu par email")
    @PostMapping("/verify-email")
    public Mono<User> verifyEmail(
            @Parameter(description = "userId et code de vérification à 6 chiffres")
            @Valid @RequestBody VerificationRequest request) {
        return authService.verifyEmail(request.getUserId(), request.getToken());
    }

    /**
     * 🔐 ENDPOINT PUBLIC - Connexion d'un utilisateur
     * Vérifie les identifiants (email + mot de passe)
     * Retourne un Access Token JWT (15 min) et un Refresh Token (7 jours)
     * Le compte doit être vérifié (is_active=true) pour se connecter
     */
    @Operation(summary = "Connexion", 
               description = "Authentifier un utilisateur et obtenir des tokens JWT")
    @PostMapping("/login")
    public Mono<JwtResponse> login(
            @Parameter(description = "Email et mot de passe")
            @Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 🔄 ENDPOINT PUBLIC - Rafraîchir l'Access Token expiré
     * Utilise le Refresh Token pour obtenir un nouveau Access Token
     * Permet de rester connecté sans redemander les identifiants
     * Le Refresh Token doit être valide et non révoqué
     */
    @Operation(summary = "Rafraîchir le token", 
               description = "Obtenir un nouveau Access Token avec le Refresh Token")
    @PostMapping("/refresh")
    public Mono<JwtResponse> refresh(
            @Parameter(description = "Refresh Token reçu lors de la connexion", example = "abc123...")
            @RequestParam("refreshToken") String refreshToken) {
        return authService.refresh(refreshToken);
    }

    /**
     * 🚪 ENDPOINT PUBLIC - Déconnexion
     * Révoque le Refresh Token pour empêcher son utilisation future
     * L'Access Token reste valide jusqu'à son expiration (15 min)
     * Bonne pratique de sécurité : toujours se déconnecter proprement
     */
    @Operation(summary = "Déconnexion", 
               description = "Révoque le Refresh Token pour sécuriser la session")
    @PostMapping("/logout")
    public Mono<Void> logout(
            @Parameter(description = "Refresh Token à révoquer", example = "abc123...")
            @RequestParam("refreshToken") String refreshToken) {
        return authService.logout(refreshToken);
    }

    /**
     * 🔑 ENDPOINT PUBLIC - Demander la réinitialisation du mot de passe
     * Génère un code de réinitialisation à 6 chiffres
     * Envoie le code par email à l'utilisateur
     * Le code expire après 1 heure
     */
    @Operation(summary = "Mot de passe oublié", 
               description = "Envoie un code de réinitialisation par email")
    @PostMapping("/forgot-password")
    public Mono<Void> forgotPassword(
            @Parameter(description = "Adresse email du compte", example = "user@example.com")
            @RequestParam("email") String email) {
        return authService.forgotPassword(email);
    }

    /**
     * 🔐 ENDPOINT PUBLIC - Réinitialiser le mot de passe avec le code reçu
     * Valide le code de réinitialisation à 6 chiffres
     * Met à jour le mot de passe du compte
     * Le code doit être valide et non expiré
     */
    @Operation(summary = "Réinitialiser le mot de passe", 
               description = "Définir un nouveau mot de passe avec le code de réinitialisation")
    @PostMapping("/reset-password")
    public Mono<Void> resetPassword(
            @Parameter(description = "Email, code de réinitialisation et nouveau mot de passe")
            @Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    /**
     * 👤 ENDPOINT UTILISATEUR - Récupérer mes informations de profil
     * Nécessite d'être authentifié (Access Token valide)
     * Retourne les informations du compte connecté
     * Utilisé pour afficher le profil utilisateur dans le frontend
     */
    @Operation(summary = "Mon profil", 
               description = "Récupère les informations du compte connecté")
    @GetMapping("/users/me")
    @PreAuthorize("isAuthenticated()")
    public Mono<UserResponse> getMe(Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return authService.getMe(userId)
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        .build());
    }

    /**
     * ✏️ ENDPOINT UTILISATEUR - Modifier mes informations de profil
     * Permet de changer : prénom, nom, téléphone
     * L'email ne peut pas être modifié (identifiant unique)
     * Le mot de passe se change via /reset-password
     */
    @Operation(summary = "Modifier mon profil", 
               description = "Met à jour les informations personnelles du compte connecté")
    @PatchMapping("/users/me")
    @PreAuthorize("isAuthenticated()")
    public Mono<User> updateMe(
            @Parameter(description = "Nouvelles données : firstName, lastName, phone")
            @Valid @RequestBody UpdateProfileRequest request, 
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return authService.updateProfile(userId, request);
    }

    /**
     * 👥 ENDPOINT ADMIN - Lister tous les utilisateurs du système
     * Affiche tous les comptes (CLIENT et ADMIN)
     * Utilisé pour le dashboard d'administration
     * Permet de voir l'état de chaque compte (actif/inactif, vérifié/non vérifié)
     */
    @Operation(summary = "[ADMIN] Lister tous les utilisateurs", 
               description = "Récupère la liste complète des utilisateurs enregistrés")
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<User> listUsers() {
        return authService.listUsers();
    }

    /**
     * 🔧 ENDPOINT ADMIN - Changer le rôle d'un utilisateur
     * Promotion CLIENT → ADMIN ou rétrogradation ADMIN → CLIENT
     * Action critique : modifie les permissions de l'utilisateur
     * Utilisé pour gérer les droits d'administration
     */
    @Operation(summary = "[ADMIN] Changer le rôle", 
               description = "Promouvoir un utilisateur en ADMIN ou rétrograder en CLIENT")
    @PatchMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<User> changeRole(
            @Parameter(description = "Identifiant UUID de l'utilisateur", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            @Parameter(description = "Nouveau rôle : ADMIN ou CLIENT")
            @Valid @RequestBody ChangeRoleRequest request) {
        return authService.changeRole(id, request.getNewRole());
    }

    /**
     * 🔴🟢 ENDPOINT ADMIN - Activer ou désactiver un compte utilisateur
     * is_active=true : L'utilisateur peut se connecter
     * is_active=false : Le compte est bloqué (suspension temporaire)
     * Utilisé pour modérer les comportements abusifs ou suspendre un compte
     */
    @Operation(summary = "[ADMIN] Changer le statut actif/inactif", 
               description = "Active ou désactive un compte utilisateur (blocage/déblocage)")
    @PatchMapping("/admin/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<User> changeStatus(
            @Parameter(description = "Identifiant UUID de l'utilisateur", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            @Parameter(description = "Nouveau statut : true (actif) ou false (bloqué)")
            @Valid @RequestBody ChangeStatusRequest request) {
        return authService.changeStatus(id, request.isActive());
    }
}