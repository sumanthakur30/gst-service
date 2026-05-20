package com.shopmanagement.gstservice.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gst_registration_branch_map")
@Getter
@Setter
public class GstRegistrationBranchMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private Long gstRegistrationId;

    private Long branchId;

    @Column(length = 64)
    private String shopId;

    @Column(nullable = false)
    private boolean isDefault = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
