package com.longdnh.donot_get_left_behind.tenancy.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.tenancy.application.port.MembershipRepository;
import com.longdnh.donot_get_left_behind.tenancy.domain.Membership;
import com.longdnh.donot_get_left_behind.tenancy.infrastructure.persistence.entities.MembershipEntity;

import jakarta.persistence.EntityManager;

@Repository
public class MembershipRepositoryAdapter implements MembershipRepository {

    private final EntityManager entityManager;

    public MembershipRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Membership create(Membership membership) {
        MembershipEntity entity = MembershipEntity.fromDomain(membership);
        entityManager.persist(entity);
        return entity.toDomain();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Optional<Membership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId) {
        // Both ids are always in the predicate: a membership is only ever addressed within its tenant.
        return entityManager.createQuery(
                        "select m from MembershipEntity m where m.organizationId = :organizationId and m.userId = :userId",
                        MembershipEntity.class)
                .setParameter("organizationId", organizationId)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .map(MembershipEntity::toDomain);
    }
}
