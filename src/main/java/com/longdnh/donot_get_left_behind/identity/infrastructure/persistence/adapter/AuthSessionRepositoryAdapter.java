package com.longdnh.donot_get_left_behind.identity.infrastructure.persistence.adapter;

import static com.longdnh.donot_get_left_behind.common.Validation.requireUuidV7;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.identity.application.port.AuthSessionRepository;
import com.longdnh.donot_get_left_behind.identity.domain.AuthSession;
import com.longdnh.donot_get_left_behind.identity.infrastructure.persistence.AuthSessionEntity;

import jakarta.persistence.EntityManager;

@Repository
@Transactional(readOnly = true)
public class AuthSessionRepositoryAdapter implements AuthSessionRepository {

    private final EntityManager entityManager;

    public AuthSessionRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public AuthSession create(AuthSession session) {
        AuthSessionEntity entity = AuthSessionEntity.fromDomain(session);

        entityManager.persist(entity);

        return entity.toDomain();
    }

    @Override
    public Optional<AuthSession> findById(UUID id) {
        UUID sessionId = requireUuidV7(id, "id");

        AuthSessionEntity entity = entityManager.find(
                AuthSessionEntity.class,
                sessionId
        );

        return Optional.ofNullable(entity)
                .map(AuthSessionEntity::toDomain);
    }
}