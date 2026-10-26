package com.longdnh.donot_get_left_behind.identity.infrastructure.security;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.longdnh.donot_get_left_behind.identity.application.port.IdGenerator;

@Component
public final class UuidV7Generator implements IdGenerator {

    private final TimeBasedEpochGenerator generator = Generators.timeBasedEpochGenerator();

    @Override
    public UUID generate() {
        return generator.generate();
    }
}
