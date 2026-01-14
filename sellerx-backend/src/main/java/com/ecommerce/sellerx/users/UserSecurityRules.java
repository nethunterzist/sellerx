package com.ecommerce.sellerx.users;

import com.ecommerce.sellerx.common.SecurityRules;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.products.TrendyolProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserSecurityRules implements SecurityRules {
    
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final TrendyolProductRepository trendyolProductRepository;
    
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers(HttpMethod.POST, "/users").permitAll();
    }
    
    public boolean canAccessStore(Authentication authentication, UUID storeId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        
        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return false;
        }
        
        return store.getUser().getId().equals(user.getId());
    }
    
    public boolean canAccessProduct(Authentication authentication, UUID productId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        
        TrendyolProduct product = trendyolProductRepository.findById(productId).orElse(null);
        if (product == null) {
            return false;
        }
        
        return product.getStore().getUser().getId().equals(user.getId());
    }
}
