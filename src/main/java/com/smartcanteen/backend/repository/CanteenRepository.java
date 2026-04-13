package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.entity.Canteen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanteenRepository extends JpaRepository<Canteen,Long> {
}
