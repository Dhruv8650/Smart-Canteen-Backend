package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.dto.response.analytics.*;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.entity.User;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {

    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN FETCH o.user
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.foodItem
        WHERE o.user.email = :email
        """)
    List<Order> findOrdersByUserEmail(@Param("email") String email);

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

    List<Order> findByStatusInOrderByCreatedAtAsc(List<OrderStatus> statuses);


    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN FETCH o.user
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.foodItem
    """)
    List<Order> findAllWithDetails();

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.user
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.foodItem
        WHERE o.status IN :statuses
    """)
    List<Order> findByStatusesWithDetails(List<OrderStatus> statuses);

    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.status IN ('PENDING', 'PREPARING', 'READY')
    """)
    long countActiveOrders();

    @Query("""
        SELECT COUNT(o) > 0
        FROM Order o
        JOIN o.orderItems oi
        WHERE o.user = :user
          AND oi.foodItem.id = :foodItemId
          AND o.status = 'COMPLETED'
    """)
    boolean hasUserOrderedItem(User user, Long foodItemId);

    Optional<Order> findByPickupCode(String pickupCode);

    @Query("""
    SELECT COUNT(o) FROM Order o
    WHERE o.status IN ('PENDING', 'PREPARING')
    OR (o.status = 'READY' AND o.pickupExpiry > :now)
""")
    long countActiveOrdersSmart(LocalDateTime now);

    @Query("""
    SELECT o FROM Order o
    LEFT JOIN FETCH o.orderItems oi
    LEFT JOIN FETCH oi.foodItem
    WHERE o.id = :id
""")
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @Query("""
    SELECT o FROM Order o
    JOIN FETCH o.user
    LEFT JOIN FETCH o.orderItems oi
    LEFT JOIN FETCH oi.foodItem
    WHERE o.pickupCode = :code
""")
    Optional<Order> findByPickupCodeWithDetails(@Param("code") String code);

    @Query("""
    SELECT DISTINCT o FROM Order o
    JOIN FETCH o.user
    LEFT JOIN FETCH o.orderItems oi
    LEFT JOIN FETCH oi.foodItem
    WHERE o.id = :id
""")
    Optional<Order> findByIdWithInvoiceDetails(@Param("id") Long id);

}
