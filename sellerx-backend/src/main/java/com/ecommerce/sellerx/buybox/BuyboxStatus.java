package com.ecommerce.sellerx.buybox;

/**
 * Buybox durumu enum'u.
 * Bir ürünün buybox'taki konumunu belirtir.
 */
public enum BuyboxStatus {
    /**
     * Buybox sizde - kazandınız
     */
    WON,

    /**
     * Buybox başka bir satıcıda - kaybettiniz
     */
    LOST,

    /**
     * Fiyat farkı belirlenen eşiğin altında - risk var
     */
    RISK,

    /**
     * Tek satıcı sizsiniz - rekabet yok
     */
    NO_COMPETITION
}
