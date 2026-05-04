package com.smartcanteen.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;


@Entity
@Getter
@Table(name = "orders") // lowercase is best practice
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many orders can belong to one user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> orderItems;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(unique = true)
    private String paymentId;

    @Column(unique = true)
    private String paymentOrderId;

    private String paymentSignature;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readyAt;

    @Column(unique = true)
    private String pickupCode; // QR content

    @Column(unique = true, name = "pickup_code_hash")
    private String pickupCodeHash;

    @Column(nullable = false)
    private Boolean qrUsed = false;

    private LocalDateTime qrUsedAt;

    private LocalDateTime pickupExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private OrderSource source = OrderSource.USER;

    @Column(nullable = false)
    private boolean hasCookedItems;

    @Column(nullable = false)
    private boolean hasReadyMadeItems;

    @Column(nullable = false)
    private Integer totalPrepTime = 0;

    @Transient
    private Double priorityScore;



    public Order() {}

    @PrePersist
    protected void onCreate() {
        if (this.source == null) {
            this.source = OrderSource.USER;
        }
        if (this.totalPrepTime == null) {
            this.totalPrepTime = 0;
        }

        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    // Setters

    public void setUser(User user) {
        this.user = user;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public void setPaymentOrderId(String paymentOrderId) {
        this.paymentOrderId = paymentOrderId;
    }

    public void setPaymentSignature(String paymentSignature) {
        this.paymentSignature = paymentSignature;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setPickupCode(String pickupCode) {
        this.pickupCode = pickupCode;
    }

    public void setPickupCodeHash(String pickupCodeHash) {
        this.pickupCodeHash = pickupCodeHash;
    }

    public void setReadyAt(LocalDateTime readyAt) {
        this.readyAt = readyAt;
    }

    public void setPickupExpiry(LocalDateTime pickupExpiry) {
        this.pickupExpiry = pickupExpiry;

    }

    public void setQrUsed(Boolean qrUsed) {
        this.qrUsed = qrUsed;
    }

    public void setQrUsedAt(LocalDateTime qrUsedAt) {
        this.qrUsedAt = qrUsedAt;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public void setSource(OrderSource source) {
        this.source = source;
    }

    public void setTotalPrepTime(Integer totalPrepTime) {
        if (totalPrepTime == null) {
            this.totalPrepTime = 0;
            return;
        }

        if (totalPrepTime < 0) {
            throw new IllegalArgumentException("totalPrepTime cannot be negative");
        }

        this.totalPrepTime = totalPrepTime;
    }

    public void setPriorityScore(Double priorityScore) {
        this.priorityScore = priorityScore;
    }


    public void setHasCookedItems(boolean hasCookedItems) {
        this.hasCookedItems = hasCookedItems;
    }

    public void setHasReadyMadeItems(boolean hasReadyMadeItems) {
        this.hasReadyMadeItems = hasReadyMadeItems;
    }

    public boolean hasCookedItems() {
        return hasCookedItems;
    }

    public boolean hasReadyMadeItems() {
        return hasReadyMadeItems;
    }

    public boolean isQrUsed() {
        return Boolean.TRUE.equals(qrUsed);
    }
}
