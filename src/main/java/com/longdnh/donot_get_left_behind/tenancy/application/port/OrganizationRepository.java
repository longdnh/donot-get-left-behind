package com.longdnh.donot_get_left_behind.tenancy.application.port;

import java.util.Optional;
import java.util.UUID;

import com.longdnh.donot_get_left_behind.common.pagination.PageRequest;
import com.longdnh.donot_get_left_behind.common.pagination.PageResult;
import com.longdnh.donot_get_left_behind.tenancy.application.dto.OrganizationSummary;
import com.longdnh.donot_get_left_behind.tenancy.domain.Organization;

public interface OrganizationRepository {

    /** Inserts a new organization. Must be called inside the caller's transaction. */
    Organization create(Organization organization);

    /** Must be called inside the caller's transaction. */
    Optional<Organization> findById(UUID organizationId);

    /** Writes the new state of an existing organization. Must be called inside the caller's transaction. */
    Organization update(Organization organization);

    /**
     * Organizations in which the user holds an ACTIVE membership, ordered by organization id
     * (UUIDv7, so creation order and a total order). Must be called inside the caller's transaction.
     */
    PageResult<OrganizationSummary> findActiveByMemberUserId(UUID userId, PageRequest page);
}
