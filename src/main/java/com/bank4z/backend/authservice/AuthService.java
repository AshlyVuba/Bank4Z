package com.bank4z.backend.authservice;

import com.bank4z.backend.accountservice.AccountService;
import com.bank4z.backend.authservice.dto.AuthResponse;
import com.bank4z.backend.authservice.dto.LoginRequest;
import com.bank4z.backend.authservice.dto.RegisterRequest;
import com.bank4z.backend.common.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AccountService accountService;

    @Value("${jwt.expiration-ms}")
    private long accessTokenExpiryMs;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AccountService accountService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accountService = accountService;
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

        // Same transaction as the user save — if account creation fails
        // (e.g. AccountNumberGenerator exhausts its retries), the whole
        // registration rolls back rather than leaving an accountless user.
        accountService.createAccountForUser(user);

        return user;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Email or password is incorrect");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Email or password is incorrect"));

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken, "refresh")) {
            throw new BadCredentialsException("Refresh token is invalid or expired");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Account no longer exists"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(accessToken, newRefreshToken, "Bearer", accessTokenExpiryMs);
    }
}