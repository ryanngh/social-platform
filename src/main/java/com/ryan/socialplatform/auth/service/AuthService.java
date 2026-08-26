// auth/service/AuthService.java
package com.ryan.socialplatform.auth.service;

import com.ryan.socialplatform.auth.dto.LoginRequest;
import com.ryan.socialplatform.auth.dto.RefreshTokenRequest;
import com.ryan.socialplatform.auth.dto.TokenResponse;
import com.ryan.socialplatform.auth.entity.UserSession;
import com.ryan.socialplatform.auth.exceptions.InvalidCredentialsException;
import com.ryan.socialplatform.auth.exceptions.InvalidRefreshTokenException;
import com.ryan.socialplatform.auth.repository.UserCredentialsRepository;
import com.ryan.socialplatform.auth.repository.UserSessionRepository;
import com.ryan.socialplatform.auth.security.CustomUserDetails;
import com.ryan.socialplatform.auth.security.JwtService;
import com.ryan.socialplatform.auth.security.RefreshTokenService;
import com.ryan.socialplatform.user.repository.UserAppRoleRepository;
import com.ryan.socialplatform.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ryan.socialplatform.auth.dto.RegisterRequest;
import com.ryan.socialplatform.auth.exceptions.AccountAlreadyExistsException;
import com.ryan.socialplatform.user.entity.User;
import com.ryan.socialplatform.user.entity.UserAppRole;
import com.ryan.socialplatform.user.entity.UserCredentials;
import com.ryan.socialplatform.user.entity.UserProfile;
import com.ryan.socialplatform.user.enums.AppRole;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final long REFRESH_TOKEN_TTL_DAYS = 30;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserSessionRepository userSessionRepository;
    private final UserAppRoleRepository userAppRoleRepository;
    private final UserRepository userRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final com.ryan.socialplatform.user.repository.UserProfileRepository userProfileRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UserSessionRepository userSessionRepository,
                       UserAppRoleRepository userAppRoleRepository,
                       UserRepository userRepository,
                       UserCredentialsRepository userCredentialsRepository,
                       com.ryan.socialplatform.user.repository.UserProfileRepository userProfileRepository,
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userSessionRepository = userSessionRepository;
        this.userAppRoleRepository = userAppRoleRepository;
        this.userRepository = userRepository;
        this.userCredentialsRepository = userCredentialsRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.identifier(), request.password()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException();
        }

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        List<String> roles = principal.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .toList();

        return issueTokens(principal.getUserId(), roles);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String hash = refreshTokenService.hash(request.refreshToken());

        UserSession session = userSessionRepository.findByRefreshTokenHash(hash)
                .filter(s -> s.getRevokedAt() == null)
                .filter(s -> s.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(InvalidRefreshTokenException::new);

        // Rotation: thu hồi session cũ trước khi phát hành session mới
        session.setRevokedAt(Instant.now());
        userSessionRepository.save(session);

        UUID userId = session.getUser().getId();
        List<String> roles = userAppRoleRepository.findAllByUserId(userId).stream()
                .map(r -> r.getRole().name())
                .toList();

        return issueTokens(userId, roles);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (request.email() != null && userCredentialsRepository.existsByEmail(request.email())) {
            throw new AccountAlreadyExistsException("Email already in use");
        }
        if (userCredentialsRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new AccountAlreadyExistsException("Phone number already in use");
        }

        User user = userRepository.saveAndFlush(User.create());

        UserCredentials credentials = new UserCredentials(
                user,
                request.phoneNumber(),
                passwordEncoder.encode(request.password())
        );
        credentials.setEmail(request.email());

        try {
            userCredentialsRepository.save(credentials);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // race condition: 2 request cùng đăng ký 1 email/phone gần như đồng thời
            throw new AccountAlreadyExistsException("Email or phone number already in use");
        }

        userProfileRepository.save(new UserProfile(user, request.firstName(), request.lastName()));
        userAppRoleRepository.save(new UserAppRole(user, AppRole.USER, null));

        return issueTokens(user.getId(), List.of(AppRole.USER.name()));
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String hash = refreshTokenService.hash(request.refreshToken());
        userSessionRepository.findByRefreshTokenHash(hash)
                .ifPresent(session -> {
                    session.setRevokedAt(Instant.now());
                    userSessionRepository.save(session);
                });
    }

    private TokenResponse issueTokens(UUID userId, List<String> roles) {
        String accessToken = jwtService.generateAccessToken(userId, roles);
        String rawRefreshToken = refreshTokenService.generateRawToken();
        String refreshHash = refreshTokenService.hash(rawRefreshToken);

        UserSession session = new UserSession(
                userRepository.getReferenceById(userId), // proxy
                refreshHash,
                Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS)
        );
        userSessionRepository.save(session);

        return TokenResponse.bearer(accessToken, rawRefreshToken);
    }
}