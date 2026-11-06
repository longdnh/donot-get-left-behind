package com.longdnh.donot_get_left_behind.tenancy.application.port;

import com.longdnh.donot_get_left_behind.tenancy.domain.Membership;

public interface MembershipRepository {

    /** Inserts a new membership. Must be called inside the caller's transaction. */
    Membership create(Membership membership);
}
