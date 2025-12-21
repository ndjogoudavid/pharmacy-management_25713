package rw.auca.ac.pharmacy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.auca.ac.pharmacy.dto.auth.AuthResponse;
import rw.auca.ac.pharmacy.dto.auth.ForgotPasswordRequest;
import rw.auca.ac.pharmacy.dto.auth.LoginRequest;
import rw.auca.ac.pharmacy.dto.auth.ResetPasswordRequest;
import rw.auca.ac.pharmacy.dto.auth.TwoFaVerifyRequest;
import rw.auca.ac.pharmacy.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<AuthResponse> verify(@Valid @RequestBody TwoFaVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyTwoFa(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
