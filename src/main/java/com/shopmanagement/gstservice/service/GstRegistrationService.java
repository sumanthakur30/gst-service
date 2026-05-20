package com.shopmanagement.gstservice.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanagement.gstservice.api.GstApi.GstRegistrationResponse;
import com.shopmanagement.gstservice.api.GstApi.GstRegistrationUpsert;
import com.shopmanagement.gstservice.exception.ConflictException;
import com.shopmanagement.gstservice.exception.NotFoundException;
import com.shopmanagement.gstservice.model.GstRegistration;
import com.shopmanagement.gstservice.model.RegistrationType;
import com.shopmanagement.gstservice.repository.GstRegistrationRepository;
import com.shopmanagement.gstservice.support.GstinValidator;
import com.shopmanagement.gstservice.support.TenantIds;

@Service
public class GstRegistrationService {

    private final GstRegistrationRepository repository;

    public GstRegistrationService(GstRegistrationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<GstRegistrationResponse> list() {
        long tenantId = TenantIds.require();
        return repository.findByTenantIdAndActiveTrueOrderByLegalNameAsc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GstRegistrationResponse get(Long id) {
        long tenantId = TenantIds.require();
        return repository.findByTenantIdAndId(tenantId, id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("GST registration not found"));
    }

    @Transactional
    public GstRegistrationResponse create(GstRegistrationUpsert body) {
        long tenantId = TenantIds.require();
        var validation = GstinValidator.validate(body.gstin());
        if (!validation.validFormat()) {
            throw new IllegalArgumentException(validation.message());
        }
        if (!validation.stateCode().equals(body.stateCode().trim())) {
            throw new IllegalArgumentException("stateCode must match first 2 digits of GSTIN");
        }
        if (repository.existsByTenantIdAndGstinIgnoreCase(tenantId, validation.normalizedGstin())) {
            throw new ConflictException("GSTIN already registered for this tenant");
        }

        GstRegistration reg = new GstRegistration();
        reg.setTenantId(tenantId);
        reg.setLegalName(body.legalName().trim());
        reg.setGstin(validation.normalizedGstin());
        reg.setStateCode(body.stateCode().trim());
        reg.setRegistrationType(body.registrationType() != null ? body.registrationType() : RegistrationType.REGULAR);
        reg.setActive(body.active() == null || body.active());
        if (body.effectiveFrom() != null) {
            reg.setEffectiveFrom(body.effectiveFrom());
        }
        reg.setCreatedAt(Instant.now());
        reg.setUpdatedAt(Instant.now());
        return toResponse(repository.save(reg));
    }

    private GstRegistrationResponse toResponse(GstRegistration reg) {
        return new GstRegistrationResponse(
                reg.getId(),
                reg.getLegalName(),
                reg.getGstin(),
                reg.getStateCode(),
                reg.getRegistrationType(),
                reg.isActive(),
                reg.getEffectiveFrom(),
                reg.getEffectiveTo());
    }
}
