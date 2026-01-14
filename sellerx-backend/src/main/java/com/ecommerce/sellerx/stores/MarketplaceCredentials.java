package com.ecommerce.sellerx.stores;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TrendyolCredentials.class, name = "trendyol")
        // Diğer marketplace credential class'ları buraya eklenebilir
})
public abstract class MarketplaceCredentials {
}
