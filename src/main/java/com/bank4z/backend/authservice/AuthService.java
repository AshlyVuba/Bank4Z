package com.bank4z.backend.authservice;

import com.bank4z.backend.accountservice.AccountService;
import com.bank4z.backend.authservice.dto.AuthResponse;
import com.bank4z.backend.authservice.dto.LoginRequest;
import com.bank4z.backend.authservice.dto.RegisterRequest;
import com.bank4z.backend.common.security.JwtService;
import com.bank4z.backend.sessionservice.SessionService;
import com.bank4z.backend.sessionservice.UserSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AccountService accountService;
    private final SessionService sessionService;

    @Value("${jwt.expiration-ms}")
    private long accessTokenExpiryMs;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AccountService accountService,
                       SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accountService = accountService;
        this.sessionService = sessionService;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException("An account with this email already exists");
        }
        if (userRepository.existsByIdNumber(request.idNumber())) {
            throw new DuplicateUserException("An account with this ID number already exists");
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicateUserException("An account with this phone number already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.password());

        User user = new User(
                request.fullName(),
                request.email(),
                request.idNumber(),
                request.phoneNumber(),
                hashedPassword
        );
        userRepository.save(user);

        // Same transaction as the user save: if account creation fails, the whole
        // registration rolls back rather than leaving an accountless user.
        accountService.createAccountForUser(user);

        return user;
    }

    /**
     * @param ipAddress the caller's address as the server sees it. We deliberately
     *                  don't trust X-Forwarded-For here: any client can forge that
     *                  header unless a known proxy is in front of the app.
     */
    public AuthResponse login(LoginRequest request, String ipAddress) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException e) {
            // Deliberately vague: never reveal whether it was the email or the password.
            throw new BadCredentialsException("Email or password is incorrect");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Email or password is incorrect"));

        // Every login is now a server-side session we can list and revoke.
        UserSession session = sessionService.startSession(
                user, request.deviceId(), request.deviceName(), ipAddress);

        return buildAuthResponse(user, session.getId(), session.getRefreshJti());
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken, "refresh")) {
            throw new BadCredentialsException("Refresh token is invalid or expired");
        }

        UUID sessionId = jwtService.extractSessionId(refreshToken);
        UUID presentedTokenId = jwtService.extractTokenId(refreshToken);
        if (sessionId == null || presentedTokenId == null) {
            throw new BadCredentialsException("Refresh token is invalid or expired");
        }

        User user = userRepository.findByEmail(jwtService.extractEmail(refreshToken))
                .orElseThrow(() -> new BadCredentialsException("Account no longer exists"));

        // Each refresh token works exactly once. Presenting an old one revokes the session.
        UUID newTokenId = sessionService.rotateRefresh(sessionId, user.getId(), presentedTokenId)
                .orElseThrow(() -> new BadCredentialsException("Session ended, log back in bestie"));

        return buildAuthResponse(user, sessionId, newTokenId);
    }

    private AuthResponse buildAuthResponse(User user, UUID sessionId, UUID refreshTokenId) {
        String accessToken = jwtService.generateAccessToken(user, sessionId);
        String refreshToken = jwtService.generateRefreshToken(user, sessionId, refreshTokenId);
        return new AuthResponse(accessToken, refreshToken, "Bearer", accessTokenExpiryMs);
    }
}