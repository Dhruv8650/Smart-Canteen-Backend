package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.dto.response.analytics.*;
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
            o.createdAt,
            SUM(o.totalAmount)
       )
       FROM Order o
       WHERE o.status = com.smartcanteen.backend.entity.OrderStatus.COMPLETED
       GROUP BY o.createdAt
       ORDER BY o.createdAt
    """)
    List<DailyRevenueDTO> getDailyRevenue();

    @Query("""
    SELECT new com.smartcanteen.backend.dto.response.analytics.WeeklyRevenueDTO(
        FUNCTION('DATE_TRUNC', 'week', o.createdAt),
        SUM(o.totalAmount)
    )
    FROM Order o
    WHERE o.status = com.smartcanteen.backend.entity.OrderStatus.COMPLETED
    GROUP BY FUNCTION('DATE_TRUNC', 'week', o.createdAt)
    ORDER BY FUNCTION('DATE_TRUNC', 'week', o.createdAt)
""")
    List<WeeklyRevenueDTO> getWeeklyRevenue();

    @Query("""
    SELECT new com.smartcanteen.backend.dto.response.analytics.MonthlyRevenueDTO(
        FUNCTION('DATE_TRUNC', 'month', o.createdAt),
        SUM(o.totalAmount)
    )
    FROM Order o
    WHERE o.status = com.smartcanteen.backend.entity.OrderStatus.COMPLETED
    GROUP BY FUNCTION('DATE_TRUNC', 'month', o.createdAt)
    ORDER BY FUNCTION('DATE_TRUNC', 'month', o.createdAt)
""")
    List<MonthlyRevenueDTO> getMonthlyRevenue();

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
            oi.foodItem.foodCategory,
            SUM(oi.quantity)
       )
       FROM OrderItem oi
       GROUP BY oi.foodItem.foodCategory
       """)
    List<CategorySalesDTO> getCategorySales();
}
