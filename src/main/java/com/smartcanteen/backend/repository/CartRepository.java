package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.entity.Cart;
import com.smartcanteen.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart,Long> {

    Optional<Cart> findByUser(User user);
}
