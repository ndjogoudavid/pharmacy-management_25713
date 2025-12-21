package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.auca.ac.pharmacy.dto.auth.AuthResponse;
import rw.auca.ac.pharmacy.dto.auth.ForgotPasswordRequest;
import rw.auca.ac.pharmacy.dto.auth.LoginRequest;
import rw.auca.ac.pharmacy.dto.auth.ResetPasswordRequest;
import rw.auca.ac.pharmacy.dto.auth.TwoFaVerifyRequest;
import rw.auca.ac.pharmacy.model.core.Person;
import rw.auca.ac.pharmacy.repository.PersonRepository;
import rw.auca.ac.pharmacy.security.JwtService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final PersonRepository personRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuthUserDetailsService userDetailsService;
    @Value("${app.auth.log-codes:false}")
    private boolean logCodes;
    @Value("${app.frontend.reset-url:http://localhost:5173/reset-password}")
    private String frontendResetUrl;

    private static final SecureRandom RANDOM = new SecureRandom();

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Person person = Optional.ofNullable(personRepository.findByEmail(request.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        person.setTwoFactorCode(code);
        person.setTwoFactorExpiresAt(LocalDateTime.now().plusMinutes(10));
        personRepository.save(person);

        if (logCodes) {
            log.info("2FA code for {} is {}", person.getEmail(), code);
        }
        String message = """
                Hello %s,

                Your pharmacy portal verification code is: %s
                This code expires in 10 minutes.

                If you did not request this, you can safely ignore this email.

                -- Pharmacy Management System
                """.formatted(person.getName(), code);
        emailService.sendEmail(person.getEmail(), "Your 2FA code", message);

        return AuthResponse.builder()
                .status("CODE_SENT")
                .email(person.getEmail())
                .name(person.getName())
                .role(person.getRole() != null ? person.getRole().name() : null)
                .build();
    }

    public AuthResponse verifyTwoFa(TwoFaVerifyRequest request) {
        Person person = Optional.ofNullable(personRepository.findByEmail(request.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (person.getTwoFactorCode() == null ||
                person.getTwoFactorExpiresAt() == null ||
                !person.getTwoFactorCode().equals(request.getCode()) ||
                person.getTwoFactorExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired code");
        }

        person.setTwoFactorCode(null);
        person.setTwoFactorExpiresAt(null);
        personRepository.save(person);

        UserDetails userDetails = userDetailsService.loadUserByUsername(person.getEmail());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .status("AUTHENTICATED")
                .token(token)
                .email(person.getEmail())
                .name(person.getName())
                .role(person.getRole() != null ? person.getRole().name() : null)
                .build();
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        Person person = personRepository.findByEmail(request.getEmail());
        if (person == null) {
            return;
        }

        String token = String.format("%06d", RANDOM.nextInt(1_000_000));
        person.setResetToken(token);
        person.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        personRepository.save(person);

        if (logCodes) {
            log.info("Password reset code for {} is {}", person.getEmail(), token);
        }
        String body = """
                Hello %s,

                We received a request to reset the password for your pharmacy account (%s).
                Use the code below to reset your password in the app:

                Reset code: %s

                You can open the reset page here: %s

                This code expires in 30 minutes. If you did not request a reset, you can ignore this email.

                -- Pharmacy Management System
                """.formatted(person.getName(), person.getEmail(), token, frontendResetUrl);
        emailService.sendEmail(person.getEmail(), "Password reset instructions", body);
    }

    public void resetPassword(ResetPasswordRequest request) {
        Person person = Optional.ofNullable(personRepository.findByResetToken(request.getToken()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (person.getResetTokenExpiresAt() == null || person.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expired");
        }

        person.setPassword(passwordEncoder.encode(request.getNewPassword()));
        person.setResetToken(null);
        person.setResetTokenExpiresAt(null);
        personRepository.save(person);
    }
}
