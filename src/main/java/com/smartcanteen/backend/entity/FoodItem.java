package com.smartcanteen.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private BigDecimal price;

    private boolean available = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FoodItem() {}


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public boolean isAvailable() { return available; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setCategory(Category category) { this.category = category; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setAvailable(boolean available) { this.available = available; }
}