package com.longdnh.donot_get_left_behind.identity.application.port;

import java.util.UUID;

public interface IdGenerator {

    /** Generates a new RFC 9562 UUIDv7 identifier. */
    UUID generate();
}
