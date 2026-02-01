package com.ecommerce.sellerx.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request to add a new payment method
 */
@Data
public class AddPaymentMethodRequest {

    @NotBlank(message = "Card holder name is required")
    private String cardHolderName;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{16}", message = "Invalid card number")
    private String cardNumber;

    @NotBlank(message = "Expiration month is required")
    @Pattern(regexp = "0[1-9]|1[0-2]", message = "Invalid expiration month")
    private String expireMonth;

    @NotBlank(message = "Expiration year is required")
    @Pattern(regexp = "20[2-9]\\d", message = "Invalid expiration year")
    private String expireYear;

    private String cardAlias;

    private boolean setAsDefault = false;
}
