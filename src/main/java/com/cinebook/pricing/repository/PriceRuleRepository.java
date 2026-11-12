package com.cinebook.pricing.repository;

import com.cinebook.pricing.entity.PriceRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRuleRepository extends JpaRepository<PriceRule, Long> {
}
