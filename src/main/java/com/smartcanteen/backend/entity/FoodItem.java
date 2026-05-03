package com.smartcanteen.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType itemType = ItemType.READY_MADE;

    @Min(0)
    @Column(nullable = false)
    private int prepTimeMinutes = 0;

    @Deprecated
    @Column(name = "is_prepared_item")
    private Boolean isPreparedItem = false;

    @Column(name = "max_per_order")
    private Integer maxPerOrder;

    public FoodItem() {}


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        normalizeItemType();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalizeItemType();
    }

    private void normalizeItemType() {
        if (itemType == null) {
            itemType = Boolean.TRUE.equals(isPreparedItem)
                    ? ItemType.COOKED
                    : ItemType.READY_MADE;
        }

        isPreparedItem = itemType == ItemType.COOKED;

        if (prepTimeMinutes < 0) {
            throw new IllegalArgumentException("prepTimeMinutes cannot be negative");
        }
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
    public Boolean getIsPreparedItem() {
        return isPreparedItem;
    }
    public Integer getMaxPerOrder() {
        return maxPerOrder;
    }
    public ItemType getItemType() {
        return itemType;
    }
    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setCategory(FoodCategory foodCategory) { this.foodCategory = foodCategory; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Deprecated
    public void setIsPreparedItem(Boolean isPreparedItem) {
        this.isPreparedItem = Boolean.TRUE.equals(isPreparedItem);
        this.itemType = Boolean.TRUE.equals(isPreparedItem)
                ? ItemType.COOKED
                : ItemType.READY_MADE;
    }
    public void setMaxPerOrder(Integer maxPerOrder) {
        this.maxPerOrder = maxPerOrder;
    }
    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
        this.isPreparedItem = itemType == ItemType.COOKED;
    }
    public void setPrepTimeMinutes(int prepTimeMinutes) {
        if (prepTimeMinutes < 0) {
            throw new IllegalArgumentException("prepTimeMinutes cannot be negative");
        }
        this.prepTimeMinutes = prepTimeMinutes;
    }
}