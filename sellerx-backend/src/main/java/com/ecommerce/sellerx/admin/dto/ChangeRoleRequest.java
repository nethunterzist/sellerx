package com.ecommerce.sellerx.admin.dto;

import com.ecommerce.sellerx.users.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleRequest {
    @NotNull(message = "Role is required")
    private Role role;
}
