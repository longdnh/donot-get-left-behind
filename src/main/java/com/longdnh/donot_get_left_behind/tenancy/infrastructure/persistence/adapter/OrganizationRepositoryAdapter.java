package com.longdnh.donot_get_left_behind.tenancy.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.longdnh.donot_get_left_behind.tenancy.application.port.OrganizationRepository;
import com.longdnh.donot_get_left_behind.tenancy.domain.Organization;
import com.longdnh.donot_get_left_behind.tenancy.infrastructure.persistence.entities.OrganizationEntity;

import jakarta.persistence.EntityManager;

@Repository
public class OrganizationRepositoryAdapter implements OrganizationRepository {

    private final EntityManager entityManager;

    public OrganizationRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Organization create(Organization organization) {
        OrganizationEntity entity = OrganizationEntity.fromDomain(organization);
        entityManager.persist(entity);
        return entity.toDomain();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Optional<Organization> findById(UUID organizationId) {
        return Optional.ofNullable(entityManager.find(OrganizationEntity.class, organizationId))
                .map(OrganizationEntity::toDomain);
    }
}
