package com.smartcanteen.backend.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TopItemDTO{
    Long foodItemId;
    String name;
    Long totalSold;
}
