package com.ecommerce.sellerx.users;

//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
public class UserDto {
    private Long id;
    private String name;
    private String email;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String phoneNumber;
    private Role role;

    // Impersonation metadata (only set during impersonated sessions)
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isImpersonated;

    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long impersonatedBy;

    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean readOnly;

    public UserDto(Long id, String name, String email, String phoneNumber, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }
}
