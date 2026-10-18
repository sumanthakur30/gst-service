package com.shopmanagement.gstservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopmanagement.gstservice.model.GstSupplyRule;

public interface GstSupplyRuleRepository extends JpaRepository<GstSupplyRule, Long> {

    List<GstSupplyRule> findByRuleCode(String ruleCode);
}
