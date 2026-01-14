package com.ecommerce.sellerx.users;

//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserDto {
    //@JsonIgnore -> bir alt satırdaki değeri göndermeyi ignore etme
    //@JsonProperty("user_id") -> bir alt satırdaki değeri düzenleme
    private Long id;
    private String name;
    private String email;
    @JsonInclude(JsonInclude.Include.NON_NULL) //alt satırdaki değer null değilse gönder
    private String phoneNumber;
    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    //private LocalDateTime createdAt;
}
