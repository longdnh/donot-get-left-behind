package com.longdnh.donot_get_left_behind.identity.infrastructure.persistence.adapter;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.identity.application.port.RefreshTokenRepository;
import com.longdnh.donot_get_left_behind.identity.domain.RefreshToken;
import com.longdnh.donot_get_left_behind.identity.infrastructure.persistence.RefreshTokenEntity;

import jakarta.persistence.EntityManager;

@Repository 
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {
    private final EntityManager entityManager;

    public RefreshTokenRepositoryAdapter(EntityManager entityManager){
        this.entityManager = entityManager;
    }

    @Override 
    @Transactional 
    public RefreshToken create(RefreshToken refreshToken){
        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.fromDomain(refreshToken);
        entityManager.persist(refreshTokenEntity);
        return refreshTokenEntity.toDomain();
    }
}
