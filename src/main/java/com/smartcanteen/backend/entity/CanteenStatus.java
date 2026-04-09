package com.smartcanteen.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CanteenStatus {

    @Id
    private Long id = 1L; // always single row

    private boolean open;
}
