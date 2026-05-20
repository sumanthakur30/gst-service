package com.shopmanagement.gstservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopmanagement.gstservice.model.StateCodeMaster;

public interface StateCodeMasterRepository extends JpaRepository<StateCodeMaster, String> {
}
