package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.entity.Cart;
import com.smartcanteen.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart,Long> {

    Optional<Cart> findByUser(User user);

    @Query("""
        SELECT DISTINCT c FROM Cart c
        LEFT JOIN FETCH c.cartItems ci
        LEFT JOIN FETCH ci.foodItem
        WHERE c.user = :user
    """)
    Optional<Cart> findByUserWithItems(User user);

}
