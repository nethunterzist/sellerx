package com.ecommerce.sellerx.stores;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.databind.ObjectMapper;

@Converter(autoApply = true)
public class MarketplaceCredentialsConverter implements AttributeConverter<MarketplaceCredentials, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(MarketplaceCredentials attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting MarketplaceCredentials to JSON", e);
        }
    }

    @Override
    public MarketplaceCredentials convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, MarketplaceCredentials.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON to MarketplaceCredentials", e);
        }
    }
}
