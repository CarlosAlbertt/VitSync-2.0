package com.vitsync.iam.infrastructure;

import com.vitsync.iam.domain.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
}
