package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem,Long> {
}
