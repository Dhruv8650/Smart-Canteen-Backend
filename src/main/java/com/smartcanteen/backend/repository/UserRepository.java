package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
}
