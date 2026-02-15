package com.ecommerce.sellerx.users;

import com.ecommerce.sellerx.common.SecurityRules;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.qa.TrendyolQuestionRepository;
import com.ecommerce.sellerx.qa.TrendyolQuestion;
import com.ecommerce.sellerx.qa.KnowledgeSuggestionRepository;
import com.ecommerce.sellerx.qa.KnowledgeSuggestion;
import com.ecommerce.sellerx.qa.QaPatternRepository;
import com.ecommerce.sellerx.qa.QaPattern;
import com.ecommerce.sellerx.qa.ConflictAlertRepository;
import com.ecommerce.sellerx.qa.ConflictAlert;
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
    private final TrendyolQuestionRepository trendyolQuestionRepository;
    private final KnowledgeSuggestionRepository knowledgeSuggestionRepository;
    private final QaPatternRepository qaPatternRepository;
    private final ConflictAlertRepository conflictAlertRepository;
    
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

    public boolean canAccessQuestion(Authentication authentication, UUID questionId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        TrendyolQuestion question = trendyolQuestionRepository.findById(questionId).orElse(null);
        if (question == null) {
            return false;
        }

        return question.getStore().getUser().getId().equals(user.getId());
    }

    public boolean canAccessSuggestion(Authentication authentication, UUID suggestionId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        KnowledgeSuggestion suggestion = knowledgeSuggestionRepository.findById(suggestionId).orElse(null);
        if (suggestion == null) {
            return false;
        }

        return suggestion.getStore().getUser().getId().equals(user.getId());
    }

    public boolean canAccessPattern(Authentication authentication, UUID patternId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        QaPattern pattern = qaPatternRepository.findById(patternId).orElse(null);
        if (pattern == null) {
            return false;
        }

        return pattern.getStore().getUser().getId().equals(user.getId());
    }

    public boolean canAccessConflict(Authentication authentication, UUID conflictId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        ConflictAlert conflict = conflictAlertRepository.findById(conflictId).orElse(null);
        if (conflict == null) {
            return false;
        }

        return conflict.getStore().getUser().getId().equals(user.getId());
    }
}
