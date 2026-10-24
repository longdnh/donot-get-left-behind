package com.longdnh.donot_get_left_behind.identity.web;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.longdnh.donot_get_left_behind.identity.application.RegisterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final RegisterService registerService;

    public AuthController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(token.getHeaderName(), token.getToken()));
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        UUID userId = registerService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(new RegisterResponse(userId));
    }
}
