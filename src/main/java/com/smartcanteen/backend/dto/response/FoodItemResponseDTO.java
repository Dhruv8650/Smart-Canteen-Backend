package com.smartcanteen.backend.dto.response;

public class FoodItemResponseDTO {
    private final Long id;
    private final String name;
    private final double price;

    public FoodItemResponseDTO(Long id,String name,double price){
        this.id=id;
        this.name=name;
        this.price=price;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}
