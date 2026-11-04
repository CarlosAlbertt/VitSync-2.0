package com.vitsync.iam.infrastructure;

import com.vitsync.iam.domain.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
}
