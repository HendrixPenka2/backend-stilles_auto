package com.Team_Pk.car_rental.auth.service;

import com.Team_Pk.car_rental.auth.dto.JwtResponse;
import com.Team_Pk.car_rental.auth.dto.LoginRequest;
import com.Team_Pk.car_rental.auth.dto.RegisterRequest;
import com.Team_Pk.car_rental.auth.dto.ResetPasswordRequest;
import com.Team_Pk.car_rental.auth.dto.UpdateProfileRequest;
import com.Team_Pk.car_rental.auth.dto.UserEvent;
import com.Team_Pk.car_rental.auth.entity.EmailVerificationToken;
import com.Team_Pk.car_rental.auth.entity.PasswordResetToken;
import com.Team_Pk.car_rental.auth.entity.RefreshToken;
import com.Team_Pk.car_rental.auth.entity.User;
import com.Team_Pk.car_rental.auth.entity.UserRole;
import com.Team_Pk.car_rental.auth.repository.EmailVerificationTokenRepository;
import com.Team_Pk.car_rental.auth.repository.PasswordResetTokenRepository;
import com.Team_Pk.car_rental.auth.repository.RefreshTokenRepository;
import com.Team_Pk.car_rental.auth.repository.UserRepository;
import com.Team_Pk.car_rental.config.RabbitMQConfig;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public Mono<User> register(RegisterRequest request) {
        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) return Mono.error(new RuntimeException("Email déjà utilisé"));
                    
                    User user = User.builder()
                            .email(request.getEmail())
                            .passwordHash(passwordEncoder.encode(request.getPassword()))
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .phone(request.getPhone())
                            .role(UserRole.CLIENT)
                            .emailVerified(false)
                            .isActive(true)
                            .build();

                    return userRepository.save(user)
                            .flatMap(this::sendVerificationEmail);
                });
    }

    private Mono<User> sendVerificationEmail(User user) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(otp)
                .expiryDate(Instant.now().plusSeconds(900))
                .build();

        return tokenRepository.save(verificationToken)
                .flatMap(savedToken -> Mono.fromRunnable(() -> {
                    try {
                        SimpleMailMessage message = new SimpleMailMessage();
                        message.setTo(user.getEmail());
                        message.setSubject("Vérification de votre email - Stilles Auto");
                        message.setText("Votre code de vérification est : " + otp);
                        mailSender.send(message);
                    } catch (Exception e) {
                        log.error("Erreur envoi email", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(user));
    }

    public Mono<User> verifyEmail(UUID userId, String token) {
        return tokenRepository.findByUserIdAndToken(userId, token)
                .flatMap(vt -> {
                    if (vt.getExpiryDate().isBefore(Instant.now())) {
                        return tokenRepository.delete(vt).then(Mono.error(new RuntimeException("Code expiré")));
                    }
                    return userRepository.findById(userId)
                            .flatMap(user -> {
                                user.setEmailVerified(true);
                                return userRepository.save(user)
                                        .flatMap(savedUser -> {
                                            // 🔥 ENVOI DE L'ÉVÉNEMENT POUR LE MAIL DE BIENVENUE
                                            UserEvent event = UserEvent.builder()
                                                    .userId(savedUser.getId())
                                                    .email(savedUser.getEmail())
                                                    .firstName(savedUser.getFirstName())
                                                    .action("VERIFIED")
                                                    .build();
                                            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.USER_VERIFIED_KEY, event);
                                            log.info("📤 Événement USER_VERIFIED envoyé pour user: {} ({})", savedUser.getId(), savedUser.getEmail());
                                        
                                            return tokenRepository.deleteByUserId(userId).thenReturn(savedUser);
                                        
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Code invalide")));
    });
    }

    public Mono<JwtResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                        return Mono.error(new RuntimeException("Identifiants invalides"));
                    }
                    if (!user.isEmailVerified()) {
                        return Mono.error(new RuntimeException("Veuillez vérifier votre email"));
                    }
                    
                    String accessToken = generateJwt(user);
                    return createOrRefreshToken(user)
                            .map(rt -> JwtResponse.builder()
                                    .accessToken(accessToken)
                                    .refreshToken(rt.getToken())
                                    .expiresIn(accessExpiration / 1000)
                                    .build());
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Utilisateur non trouvé")));
    }

    /**
     * MODIFICATION ICI : Utilisation de getBytes() au lieu de Base64
     */
    private String generateJwt(User user) {
        // On utilise directement les octets de la chaîne de caractères
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(key)
                .compact();
    }

    private Mono<RefreshToken> createOrRefreshToken(User user) {
        return refreshTokenRepository.findByUserId(user.getId())
                .flatMap(existing -> {
                    existing.setToken(UUID.randomUUID().toString());
                    existing.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
                    existing.setRevoked(false);
                    return refreshTokenRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> refreshTokenRepository.save(
                        RefreshToken.builder()
                                .userId(user.getId())
                                .token(UUID.randomUUID().toString())
                                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                                .build()
                )));
    }

    public Mono<JwtResponse> refresh(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(rt -> !rt.isRevoked() && rt.getExpiryDate().isAfter(Instant.now()))
                .flatMap(rt -> userRepository.findById(rt.getUserId())
                        .map(user -> JwtResponse.builder()
                                .accessToken(generateJwt(user))
                                .refreshToken(token)
                                .expiresIn(accessExpiration / 1000)
                                .build()))
                .switchIfEmpty(Mono.error(new RuntimeException("Token invalide ou expiré")));
    }

    public Mono<Void> logout(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .flatMap(token -> {
                    token.setRevoked(true);
                    return refreshTokenRepository.save(token).then();
                })
                .switchIfEmpty(Mono.empty());
    }

    public Mono<Void> forgotPassword(String email) {
        return userRepository.findByEmail(email)
            .flatMap(user -> {
                String token = UUID.randomUUID().toString();
                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .userId(user.getId())
                        .token(token)
                        .expiryDate(Instant.now().plusSeconds(3600))
                        .build();
                
                // On sauvegarde d'abord le token en base
                return resetTokenRepository.deleteByUserId(user.getId())
                    .then(resetTokenRepository.save(resetToken))
                    .doOnSuccess(savedToken -> log.info("✅ Token sauvegardé en base: {}", savedToken.getToken()))
                    .doOnError(error -> log.error("❌ Erreur save token:", error))
                    // On envoie l'email APRÈS, et même si ça échoue, le token reste sauvegardé
                    .flatMap(savedToken -> sendResetEmail(email, token)
                        .doOnSuccess(v -> log.info("📧 Email envoyé à: {}", email))
                        .doOnError(error -> log.warn("⚠️  Email non envoyé (mais token sauvegardé): {}", error.getMessage()))
                        .onErrorResume(error -> Mono.empty()) // Si email échoue, on continue quand même
                    );
            })
            .then()
            .switchIfEmpty(Mono.empty()); 
    }

    private Mono<Void> sendResetEmail(String email, String token) {
        return Mono.fromRunnable(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("Réinitialisation de mot de passe - Stilles Auto");
                message.setText("Code reset : " + token);
                mailSender.send(message);
            } catch (Exception e) {
                log.error("Erreur mail reset", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<Void> resetPassword(ResetPasswordRequest request) {
        // Nettoyer le token (enlever les espaces avant/après)
        String cleanToken = request.getToken().trim();
        log.info("🔍 Recherche du token: [{}] (longueur: {})", cleanToken, cleanToken.length());
        
        return resetTokenRepository.findByToken(cleanToken)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("❌ Token NOT FOUND dans la base: [{}]", cleanToken);
                    return Mono.error(new RuntimeException("Token invalide"));
                }))
                .doOnNext(token -> log.info("✅ Token trouvé dans la base pour user: {}", token.getUserId()))
                .flatMap(token -> {
                    if (token.getExpiryDate().isBefore(Instant.now())) {
                        log.warn("⏰ Token expiré: {}", token.getToken());
                        return resetTokenRepository.delete(token)
                                .then(Mono.error(new RuntimeException("Token expiré")));
                    }
                    log.info("🔐 Réinitialisation du mot de passe pour user: {}", token.getUserId());
                    return userRepository.findById(token.getUserId())
                            .flatMap(user -> {
                                user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
                                user.setUpdatedAt(Instant.now());
                                return userRepository.save(user)
                                        .flatMap(savedUser -> {
                                            // 🔥 ENVOI DE L'ÉVÉNEMENT DE SÉCURITÉ
                                            UserEvent event = UserEvent.builder()
                                                    .userId(savedUser.getId())
                                                    .email(savedUser.getEmail())
                                                    .firstName(savedUser.getFirstName())
                                                    .action("PASSWORD_CHANGED")
                                                    .build();
                                            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PASSWORD_CHANGED_KEY, event);
                                            log.info("📤 Événement PASSWORD_CHANGED envoyé pour user: {}", savedUser.getId());
                                            
                                            return refreshTokenRepository.deleteByUserId(user.getId())
                                                    .then(resetTokenRepository.delete(token));
                                        })
                                        .doOnSuccess(v -> log.info("✅ Mot de passe réinitialisé avec succès"));
                            });
                })
                .then();
    }
    //pour recupérer les infos de l'utilisateur connecté
    public Mono<User> getMe(UUID userId) {
        return userRepository.findById(userId);
    }

    //modifier les infos de l'utilisateur connecté
    public Mono<User> updateProfile(UUID userId, UpdateProfileRequest request) {
    return userRepository.findById(userId)
            .flatMap(user -> {
                if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
                if (request.getLastName() != null) user.setLastName(request.getLastName());
                if (request.getPhone() != null) user.setPhone(request.getPhone());
                if (request.getNewPassword() != null) user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
                user.setUpdatedAt(Instant.now());
                return userRepository.save(user);
            });
    }

    //recuperer la liste des utilisateurs (admin)
    public Flux<User> listUsers() {
    return userRepository.findAll();
    }

    //changer le role d'un utilisateur (admin)
    public Mono<User> changeRole(UUID id, UserRole newRole) {
    return userRepository.findById(id)
            .flatMap(user -> {
                user.setRole(newRole);
                user.setUpdatedAt(Instant.now());
                return userRepository.save(user);
            });
    }

    //changer le status d'un utilisateur (admin)
    public Mono<User> changeStatus(UUID id, boolean active) {
    return userRepository.findById(id)
            .flatMap(user -> {
                user.setActive(active);
                user.setUpdatedAt(Instant.now());
                return userRepository.save(user);
            });
    }
}