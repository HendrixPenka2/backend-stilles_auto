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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-email")
    public Mono<User> verifyEmail(@Valid @RequestBody VerificationRequest request) {
        // On passe maintenant les deux paramètres attendus par le service pro
        return authService.verifyEmail(request.getUserId(), request.getToken());
    }

    @PostMapping("/login")
    public Mono<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public Mono<JwtResponse> refresh(@RequestParam String refreshToken) {
        return authService.refresh(refreshToken);
    }

    @PostMapping("/logout")
    public Mono<Void> logout(@RequestParam String refreshToken) {
        return authService.logout(refreshToken);
    }

    @PostMapping("/forgot-password")
    public Mono<Void> forgotPassword(@RequestParam String email) {
        return authService.forgotPassword(email);
    }

    @PostMapping("/reset-password")
    public Mono<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

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

    @PatchMapping("/users/me")
    @PreAuthorize("isAuthenticated()")
    public Mono<User> updateMe(@Valid @RequestBody UpdateProfileRequest request, Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return authService.updateProfile(userId, request);
    }

    //role admin de recuperation de tous les utilisateurs
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<User> listUsers() {
        return authService.listUsers();
    }

    //controller admin pour changer le role d'un utilisateur
    @PatchMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<User> changeRole(@PathVariable UUID id, @Valid @RequestBody ChangeRoleRequest request) {
        return authService.changeRole(id, request.getNewRole());
    }

    //changement de status actif/inactif d'un utilisateur
    @PatchMapping("/admin/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<User> changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        return authService.changeStatus(id, request.isActive());
    }
}