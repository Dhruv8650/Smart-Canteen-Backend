package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.entity.CanteenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanteenStatusRepository extends JpaRepository<CanteenStatus, Long> {
}