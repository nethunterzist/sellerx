package com.ecommerce.sellerx.currency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Client for fetching exchange rates from TCMB (Turkish Central Bank).
 * TCMB provides free XML feed with daily exchange rates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TcmbApiClient {

    // TCMB's official daily exchange rates XML endpoint
    private static final String TCMB_URL = "https://www.tcmb.gov.tr/kurlar/today.xml";

    // Fallback values in case TCMB is unavailable (approximate rates as of Jan 2025)
    private static final BigDecimal FALLBACK_USD_TRY = new BigDecimal("34.5");
    private static final BigDecimal FALLBACK_EUR_TRY = new BigDecimal("37.2");

    private final RestTemplate restTemplate;

    /**
     * Fetch current exchange rates from TCMB.
     * Returns fallback values if TCMB is unavailable.
     */
    public TcmbRates fetchRates() {
        try {
            log.info("Fetching exchange rates from TCMB: {}", TCMB_URL);
            String xml = restTemplate.getForObject(TCMB_URL, String.class);

            if (xml == null || xml.isEmpty()) {
                log.warn("Empty response from TCMB, using fallback rates");
                return getFallbackRates();
            }

            TcmbRates rates = parseXml(xml);
            log.info("Successfully fetched rates from TCMB: USD/TRY={}, EUR/TRY={}",
                    rates.getUsdTry(), rates.getEurTry());
            return rates;

        } catch (Exception e) {
            log.error("Failed to fetch rates from TCMB: {}", e.getMessage());
            return getFallbackRates();
        }
    }

    /**
     * Parse TCMB XML response to extract USD and EUR rates
     */
    private TcmbRates parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Security: Disable external entities to prevent XXE attacks
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        Document doc = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        BigDecimal usdTry = null;
        BigDecimal eurTry = null;

        NodeList currencies = doc.getElementsByTagName("Currency");
        for (int i = 0; i < currencies.getLength(); i++) {
            Element currency = (Element) currencies.item(i);
            String code = currency.getAttribute("CurrencyCode");

            // TCMB uses "ForexSelling" for bank selling rate (what customers buy at)
            NodeList forexSelling = currency.getElementsByTagName("ForexSelling");
            if (forexSelling.getLength() > 0) {
                String rateStr = forexSelling.item(0).getTextContent();
                if (rateStr != null && !rateStr.trim().isEmpty()) {
                    BigDecimal rate = new BigDecimal(rateStr.trim());
                    if ("USD".equals(code)) {
                        usdTry = rate;
                    } else if ("EUR".equals(code)) {
                        eurTry = rate;
                    }
                }
            }
        }

        // Use fallbacks if parsing failed for any currency
        return TcmbRates.builder()
                .usdTry(usdTry != null ? usdTry : FALLBACK_USD_TRY)
                .eurTry(eurTry != null ? eurTry : FALLBACK_EUR_TRY)
                .build();
    }

    /**
     * Get fallback rates when TCMB is unavailable
     */
    private TcmbRates getFallbackRates() {
        log.warn("Using fallback exchange rates: USD/TRY={}, EUR/TRY={}",
                FALLBACK_USD_TRY, FALLBACK_EUR_TRY);
        return TcmbRates.builder()
                .usdTry(FALLBACK_USD_TRY)
                .eurTry(FALLBACK_EUR_TRY)
                .build();
    }
}
