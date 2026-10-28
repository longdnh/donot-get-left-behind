package com.longdnh.donot_get_left_behind.identity.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.longdnh.donot_get_left_behind.common.constants.ClientMessages;
import com.longdnh.donot_get_left_behind.identity.application.LoginResult;
import com.longdnh.donot_get_left_behind.identity.application.LoginService;
import com.longdnh.donot_get_left_behind.identity.application.RegisterService;
import com.longdnh.donot_get_left_behind.identity.application.port.UserRepository;
import com.longdnh.donot_get_left_behind.identity.web.dto.CurrentUserResponse;
import com.longdnh.donot_get_left_behind.identity.web.dto.LoginRequest;
import com.longdnh.donot_get_left_behind.identity.web.dto.LoginResponse;
import com.longdnh.donot_get_left_behind.identity.web.dto.RegisterRequest;
import com.longdnh.donot_get_left_behind.identity.web.dto.RegisterResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping(
    path = "/api/auth",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final RegisterService registerService;
    private final LoginService loginService;
    private final UserRepository userRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final Clock clock;
    private final boolean secureRefreshCookie;

    public AuthController(
            RegisterService registerService,
            LoginService loginService,
            UserRepository userRepository,
            CsrfTokenRepository csrfTokenRepository,
            Clock clock,
            @Value("${app.security.refresh.secure-cookie:true}")
            boolean secureRefreshCookie) {

        this.registerService = registerService;
        this.loginService = loginService;
        this.userRepository = userRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.clock = clock;
        this.secureRefreshCookie = secureRefreshCookie;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfResponse(
                        token.getHeaderName(),
                        token.getToken()
                ));
    }

    @PostMapping(
        path = "/register",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UUID userId = registerService.register(
                request.email(),
                request.password()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(new RegisterResponse(userId));
    }

    @PostMapping(
        path = "/login",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        LoginResult result = loginService.login(
                request.email(),
                request.password()
        );

        Instant now = clock.instant();

        long refreshMaxAge = Math.max(
                0,
                Duration.between(
                        now,
                        result.refreshTokenExpiresAt()
                ).getSeconds()
        );

        ResponseCookie refreshCookie = ResponseCookie
                .from(REFRESH_COOKIE_NAME, result.refreshToken())
                .httpOnly(true)
                .secure(secureRefreshCookie)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(refreshMaxAge)
                .build();

        servletResponse.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshCookie.toString()
        );

        csrfTokenRepository.saveToken(
                null,
                servletRequest,
                servletResponse
        );

        long expiresIn = Math.max(
                0,
                Duration.between(
                        now,
                        result.accessTokenExpiresAt()
                ).getSeconds()
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new LoginResponse(
                        result.accessToken(),
                        "Bearer",
                        expiresIn
                ));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        var user = userRepository.findById(userId)
                .filter(candidate -> candidate.isActive())
                .orElseThrow(() -> new BadCredentialsException(
                        ClientMessages.AUTHENTICATION_REQUIRED
                ));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CurrentUserResponse(
                        user.getId(),
                        user.getEmail()
                ));
    }
}