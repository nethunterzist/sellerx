package com.ecommerce.sellerx.billing.iyzico;

import com.ecommerce.sellerx.billing.config.IyzicoConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * iyzico Payment Gateway API Client
 *
 * Handles all communication with iyzico REST API including:
 * - Payment processing
 * - Card storage (tokenization)
 * - Refunds
 * - 3D Secure payments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IyzicoApiClient {

    private final IyzicoConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Create a payment with stored card token
     */
    public IyzicoPaymentResult createPayment(IyzicoPaymentRequest request) {
        log.info("Creating iyzico payment: conversationId={}, amount={}",
                request.getConversationId(), request.getPrice());

        String endpoint = "/payment/auth";
        Map<String, Object> payload = buildPaymentPayload(request);

        try {
            Map<String, Object> response = makeApiCall(endpoint, payload);
            return parsePaymentResponse(response);
        } catch (Exception e) {
            log.error("iyzico payment failed: conversationId={}", request.getConversationId(), e);
            return IyzicoPaymentResult.failure("NETWORK_ERROR", e.getMessage());
        }
    }

    /**
     * Create a 3D Secure payment (redirects user to bank)
     */
    public IyzicoThreeDsResult initiate3DSecure(IyzicoPaymentRequest request) {
        log.info("Initiating 3DS payment: conversationId={}", request.getConversationId());

        String endpoint = "/payment/3dsecure/initialize";
        Map<String, Object> payload = buildPaymentPayload(request);
        payload.put("callbackUrl", config.getCallbackUrl() + "/3ds-callback");

        try {
            Map<String, Object> response = makeApiCall(endpoint, payload);
            return parseThreeDsResponse(response);
        } catch (Exception e) {
            log.error("iyzico 3DS initiation failed: conversationId={}", request.getConversationId(), e);
            return IyzicoThreeDsResult.failure("NETWORK_ERROR", e.getMessage());
        }
    }

    /**
     * Complete 3D Secure payment after bank redirect
     */
    public IyzicoPaymentResult complete3DSecure(String paymentId) {
        log.info("Completing 3DS payment: paymentId={}", paymentId);

        String endpoint = "/payment/3dsecure/auth";
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", paymentId);

        try {
            Map<String, Object> response = makeApiCall(endpoint, payload);
            return parsePaymentResponse(response);
        } catch (Exception e) {
            log.error("iyzico 3DS completion failed: paymentId={}", paymentId, e);
            return IyzicoPaymentResult.failure("NETWORK_ERROR", e.getMessage());
        }
    }

    /**
     * Store a card for future payments (tokenization)
     */
    public IyzicoCardResult storeCard(IyzicoCardRequest request) {
        log.info("Storing card for user: userKey={}", request.getCardUserKey());

        String endpoint = "/cardstorage/card";
        Map<String, Object> payload = buildCardPayload(request);

        try {
            Map<String, Object> response = makeApiCall(endpoint, payload);
            return parseCardResponse(response);
        } catch (Exception e) {
            log.error("iyzico card storage failed", e);
            return IyzicoCardResult.failure("NETWORK_ERROR", e.getMessage());
        }
    }

    /**
     * Delete a stored card
     */
    public boolean deleteCard(String cardUserKey, String cardToken) {
        log.info("Deleting card: cardToken={}", cardToken);

        String endpoint = "/cardstorage/card";
        Map<String, Object> payload = new HashMap<>();
        payload.put("cardUserKey", cardUserKey);
        payload.put("cardToken", cardToken);

        try {
            Map<String, Object> response = makeApiDeleteCall(endpoint, payload);
            String status = (String) response.get("status");
            return "success".equalsIgnoreCase(status);
        } catch (Exception e) {
            log.error("iyzico card deletion failed: cardToken={}", cardToken, e);
            return false;
        }
    }

    /**
     * Get stored cards for a user
     */
    public List<IyzicoCardInfo> getStoredCards(String cardUserKey) {
        log.info("Fetching stored cards: cardUserKey={}", cardUserKey);

        String endpoint = "/cardstorage/cards";
        Map<String, Object> payload = new HashMap<>();
        payload.put("cardUserKey", cardUserKey);

        try {
            Map<String, Object> response = makeApiCall(endpoint, payload);
            return parseCardsResponse(response);
        } catch (Exception e) {
            log.error("iyzico fetch cards failed", e);
            return Collections.emptyList();
        }
    }

    /**
     * Refund a payment
     */
    public IyzicoRefundResult refundPayment(String paymentTransactionId, BigDecimal amount, String ip) {
        log.info("Refunding payment: paymentTransactionId={}, amount={}",
                paymentTransactionId, amount);

        String endpoint = "/payment/refund";
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentTransactionId", paymentTransactionId);
        payload.put("price", amount.toString());
        payload.put("ip", ip);
        payload.put("currency", "TRY");

        try {
            Map<String, Object> response = makeApiCall(endpoint, payload);
            return parseRefundResponse(response);
        } catch (Exception e) {
            log.error("iyzico refund failed: paymentTransactionId={}", paymentTransactionId, e);
            return IyzicoRefundResult.failure("NETWORK_ERROR", e.getMessage());
        }
    }

    /**
     * Retrieve payment details
     */
    public IyzicoPaymentResult retrievePayment(String paymentId, String conversationId) {
        log.info("Retrieving payment: paymentId={}", paymentId);

        String endpoint = "/payment/detail";
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", paymentId);
        payload.put("conversationId", conversationId);

        try {
            Map<String, Object> response = makeApiCall(endpoint, payload);
            return parsePaymentResponse(response);
        } catch (Exception e) {
            log.error("iyzico payment retrieval failed: paymentId={}", paymentId, e);
            return IyzicoPaymentResult.failure("NETWORK_ERROR", e.getMessage());
        }
    }

    // =========== Private helper methods ===========

    private Map<String, Object> buildPaymentPayload(IyzicoPaymentRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("locale", "tr");
        payload.put("conversationId", request.getConversationId());
        payload.put("price", request.getPrice().toString());
        payload.put("paidPrice", request.getPaidPrice().toString());
        payload.put("currency", "TRY");
        payload.put("installment", 1);
        payload.put("paymentChannel", "WEB");
        payload.put("paymentGroup", "SUBSCRIPTION");

        // Payment card (token or full card details)
        Map<String, Object> paymentCard = new HashMap<>();
        if (request.getCardToken() != null) {
            paymentCard.put("cardUserKey", request.getCardUserKey());
            paymentCard.put("cardToken", request.getCardToken());
        } else {
            paymentCard.put("cardHolderName", request.getCardHolderName());
            paymentCard.put("cardNumber", request.getCardNumber());
            paymentCard.put("expireMonth", request.getExpireMonth());
            paymentCard.put("expireYear", request.getExpireYear());
            paymentCard.put("cvc", request.getCvc());
            paymentCard.put("registerCard", request.isRegisterCard() ? 1 : 0);
        }
        payload.put("paymentCard", paymentCard);

        // Buyer info
        Map<String, Object> buyer = new HashMap<>();
        buyer.put("id", request.getBuyerId());
        buyer.put("name", request.getBuyerName());
        buyer.put("surname", request.getBuyerSurname());
        buyer.put("email", request.getBuyerEmail());
        buyer.put("identityNumber", request.getBuyerIdentityNumber());
        buyer.put("registrationAddress", request.getBuyerAddress());
        buyer.put("city", request.getBuyerCity());
        buyer.put("country", "Turkey");
        buyer.put("ip", request.getBuyerIp());
        payload.put("buyer", buyer);

        // Shipping address (same as billing for subscriptions)
        Map<String, Object> address = new HashMap<>();
        address.put("contactName", request.getBuyerName() + " " + request.getBuyerSurname());
        address.put("city", request.getBuyerCity());
        address.put("country", "Turkey");
        address.put("address", request.getBuyerAddress());
        payload.put("shippingAddress", address);
        payload.put("billingAddress", address);

        // Basket items
        List<Map<String, Object>> basketItems = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("id", request.getItemId());
        item.put("name", request.getItemName());
        item.put("category1", "Subscription");
        item.put("itemType", "VIRTUAL");
        item.put("price", request.getPrice().toString());
        basketItems.add(item);
        payload.put("basketItems", basketItems);

        return payload;
    }

    private Map<String, Object> buildCardPayload(IyzicoCardRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("locale", "tr");
        payload.put("conversationId", UUID.randomUUID().toString());

        if (request.getCardUserKey() != null) {
            payload.put("cardUserKey", request.getCardUserKey());
        } else {
            payload.put("email", request.getEmail());
            payload.put("externalId", request.getExternalId());
        }

        Map<String, Object> card = new HashMap<>();
        card.put("cardAlias", request.getCardAlias());
        card.put("cardHolderName", request.getCardHolderName());
        card.put("cardNumber", request.getCardNumber());
        card.put("expireMonth", request.getExpireMonth());
        card.put("expireYear", request.getExpireYear());
        payload.put("card", card);

        return payload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> makeApiCall(String endpoint, Map<String, Object> payload) {
        String url = config.getEffectiveBaseUrl() + endpoint;
        HttpHeaders headers = createAuthHeaders(payload);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            log.debug("iyzico API call: endpoint={}", endpoint);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("iyzico API call failed: " + endpoint, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> makeApiDeleteCall(String endpoint, Map<String, Object> payload) {
        String url = config.getEffectiveBaseUrl() + endpoint;
        HttpHeaders headers = createAuthHeaders(payload);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("iyzico API delete call failed: " + endpoint, e);
        }
    }

    private HttpHeaders createAuthHeaders(Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // iyzico authorization header
        String randomString = generateRandomString();
        String authorizationString = generateAuthorizationString(payload, randomString);
        headers.set("Authorization", authorizationString);

        return headers;
    }

    private String generateAuthorizationString(Map<String, Object> payload, String randomString) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String hashString = config.getApiKey() + randomString + config.getSecretKey() + payloadJson;
            String hash = DigestUtils.sha1Hex(hashString);
            String authString = config.getApiKey() + ":" + hash + ":" + randomString;
            return "IYZWS " + Base64.encodeBase64String(authString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate iyzico authorization", e);
        }
    }

    private String generateRandomString() {
        return Instant.now().toEpochMilli() + "" + new Random().nextInt(100000);
    }

    @SuppressWarnings("unchecked")
    private IyzicoPaymentResult parsePaymentResponse(Map<String, Object> response) {
        String status = (String) response.get("status");

        if ("success".equalsIgnoreCase(status)) {
            return IyzicoPaymentResult.builder()
                    .success(true)
                    .paymentId((String) response.get("paymentId"))
                    .conversationId((String) response.get("conversationId"))
                    .paidPrice(new BigDecimal(response.get("paidPrice").toString()))
                    .rawResponse(response)
                    .build();
        } else {
            String errorCode = (String) response.get("errorCode");
            String errorMessage = (String) response.get("errorMessage");
            return IyzicoPaymentResult.failure(errorCode, errorMessage);
        }
    }

    private IyzicoThreeDsResult parseThreeDsResponse(Map<String, Object> response) {
        String status = (String) response.get("status");

        if ("success".equalsIgnoreCase(status)) {
            return IyzicoThreeDsResult.builder()
                    .success(true)
                    .htmlContent((String) response.get("threeDSHtmlContent"))
                    .build();
        } else {
            String errorCode = (String) response.get("errorCode");
            String errorMessage = (String) response.get("errorMessage");
            return IyzicoThreeDsResult.failure(errorCode, errorMessage);
        }
    }

    private IyzicoCardResult parseCardResponse(Map<String, Object> response) {
        String status = (String) response.get("status");

        if ("success".equalsIgnoreCase(status)) {
            return IyzicoCardResult.builder()
                    .success(true)
                    .cardUserKey((String) response.get("cardUserKey"))
                    .cardToken((String) response.get("cardToken"))
                    .cardAlias((String) response.get("cardAlias"))
                    .binNumber((String) response.get("binNumber"))
                    .lastFourDigits((String) response.get("lastFourDigits"))
                    .cardType((String) response.get("cardType"))
                    .cardAssociation((String) response.get("cardAssociation"))
                    .cardFamily((String) response.get("cardFamily"))
                    .cardBankName((String) response.get("cardBankName"))
                    .build();
        } else {
            String errorCode = (String) response.get("errorCode");
            String errorMessage = (String) response.get("errorMessage");
            return IyzicoCardResult.failure(errorCode, errorMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private List<IyzicoCardInfo> parseCardsResponse(Map<String, Object> response) {
        String status = (String) response.get("status");
        if (!"success".equalsIgnoreCase(status)) {
            return Collections.emptyList();
        }

        List<IyzicoCardInfo> cards = new ArrayList<>();
        List<Map<String, Object>> cardDetails = (List<Map<String, Object>>) response.get("cardDetails");

        if (cardDetails != null) {
            for (Map<String, Object> card : cardDetails) {
                cards.add(IyzicoCardInfo.builder()
                        .cardToken((String) card.get("cardToken"))
                        .cardAlias((String) card.get("cardAlias"))
                        .binNumber((String) card.get("binNumber"))
                        .lastFourDigits((String) card.get("lastFourDigits"))
                        .cardType((String) card.get("cardType"))
                        .cardAssociation((String) card.get("cardAssociation"))
                        .cardFamily((String) card.get("cardFamily"))
                        .cardBankName((String) card.get("cardBankName"))
                        .build());
            }
        }

        return cards;
    }

    private IyzicoRefundResult parseRefundResponse(Map<String, Object> response) {
        String status = (String) response.get("status");

        if ("success".equalsIgnoreCase(status)) {
            return IyzicoRefundResult.builder()
                    .success(true)
                    .paymentId((String) response.get("paymentId"))
                    .paymentTransactionId((String) response.get("paymentTransactionId"))
                    .price(new BigDecimal(response.get("price").toString()))
                    .build();
        } else {
            String errorCode = (String) response.get("errorCode");
            String errorMessage = (String) response.get("errorMessage");
            return IyzicoRefundResult.failure(errorCode, errorMessage);
        }
    }
}
