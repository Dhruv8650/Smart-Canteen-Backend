package com.smartcanteen.backend.dto.request;

import com.smartcanteen.backend.entity.Role;
import lombok.Data;

@Data
public class UpdateUserRoleDTO {
    private Role role;
}
