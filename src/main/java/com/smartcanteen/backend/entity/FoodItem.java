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
    private FoodCategory foodCategory;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "image_url")
    private String imageUrl;

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
    public FoodCategory getCategory() { return foodCategory; }
    public BigDecimal getPrice() { return price; }
    public boolean isAvailable() { return available; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getImageUrl() {
        return imageUrl;
    }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setCategory(FoodCategory foodCategory) { this.foodCategory = foodCategory; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}