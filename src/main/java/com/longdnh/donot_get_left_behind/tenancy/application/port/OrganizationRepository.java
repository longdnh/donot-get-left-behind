package com.longdnh.donot_get_left_behind.tenancy.application.port;

import com.longdnh.donot_get_left_behind.tenancy.domain.Organization;

public interface OrganizationRepository {

    /** Inserts a new organization. Must be called inside the caller's transaction. */
    Organization create(Organization organization);
}
