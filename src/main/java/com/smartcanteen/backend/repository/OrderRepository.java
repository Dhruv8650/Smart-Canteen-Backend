package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.dto.response.analytics.CategorySalesDTO;
import com.smartcanteen.backend.dto.response.analytics.DailyRevenueDTO;
import com.smartcanteen.backend.dto.response.analytics.OrderStatusCountDTO;
import com.smartcanteen.backend.dto.response.analytics.TopItemDTO;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order> findByUser(User user);

    List<Order> findByStatus(OrderStatus status);

    @Query("""
       SELECT new com.smartcanteen.backend.dto.response.analytics.DailyRevenueDTO(
            DATE(o.createdAt),
            SUM(o.totalAmount)
       )
       FROM Order o
       WHERE o.status = 'COMPLETED'
       GROUP BY DATE(o.createdAt)
       ORDER BY DATE(o.createdAt)
       """)
    List<DailyRevenueDTO> getDailyRevenue();

    @Query("""
       SELECT new com.smartcanteen.backend.dto.response.analytics.OrderStatusCountDTO(
            o.status,
            COUNT(o)
       )
       FROM Order o
       GROUP BY o.status
       """)
    List<OrderStatusCountDTO> getOrderStatusCounts();

    @Query("""
       SELECT new com.smartcanteen.backend.dto.response.analytics.TopItemDTO(
            oi.foodItem.id,
            oi.foodItem.name,
            SUM(oi.quantity)
       )
       FROM OrderItem oi
       GROUP BY oi.foodItem.id, oi.foodItem.name
       ORDER BY SUM(oi.quantity) DESC
       """)
    List<TopItemDTO> getTopSellingItems();

    @Query("""
       SELECT new com.smartcanteen.backend.dto.response.analytics.CategorySalesDTO(
            oi.foodItem.category,
            SUM(oi.quantity)
       )
       FROM OrderItem oi
       GROUP BY oi.foodItem.category
       """)
    List<CategorySalesDTO> getCategorySales();
}
