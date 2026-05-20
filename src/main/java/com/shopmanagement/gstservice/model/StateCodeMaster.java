package com.shopmanagement.gstservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "state_code_master")
public class StateCodeMaster {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "union_territory", nullable = false)
    private boolean unionTerritory;

    @Column(nullable = false)
    private boolean active = true;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isUnionTerritory() {
        return unionTerritory;
    }

    public boolean isActive() {
        return active;
    }
}
