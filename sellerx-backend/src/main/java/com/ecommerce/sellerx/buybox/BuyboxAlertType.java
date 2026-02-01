package com.ecommerce.sellerx.buybox;

/**
 * Buybox alert tipleri.
 * Kullanıcıya gönderilecek uyarı türlerini belirtir.
 */
public enum BuyboxAlertType {
    /**
     * Buybox kaybedildi - başka satıcı kazandı
     */
    BUYBOX_LOST,

    /**
     * Buybox kazanıldı - artık sizde
     */
    BUYBOX_WON,

    /**
     * Yeni rakip eklendi
     */
    NEW_COMPETITOR,

    /**
     * Fiyat riski - fark eşiğin altına düştü
     */
    PRICE_RISK
}
