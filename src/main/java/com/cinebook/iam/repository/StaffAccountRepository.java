package com.cinebook.iam.repository;

import com.cinebook.iam.entity.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, Long> {
}
