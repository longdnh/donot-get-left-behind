package com.longdnh.donot_get_left_behind.tenancy.web;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.longdnh.donot_get_left_behind.tenancy.application.dto.CreateOrganizationResult;
import com.longdnh.donot_get_left_behind.tenancy.application.service.OrganizationService;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.CreateOrganizationRequest;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.CreateOrganizationResponse;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.MembershipResponse;
import com.longdnh.donot_get_left_behind.tenancy.web.dto.OrganizationResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/organizations", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateOrganizationResponse> create(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // The owner always comes from the authenticated token, never from the request body.
        UUID ownerUserId = UUID.fromString(jwt.getSubject());

        CreateOrganizationResult result = organizationService.create(ownerUserId, request.name());

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(new CreateOrganizationResponse(
                        OrganizationResponse.from(result.organization()),
                        MembershipResponse.from(result.ownerMembership())));
    }
}
