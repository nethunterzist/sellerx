package com.ecommerce.sellerx.products;

public enum ProductSyncResult {
    NEW,      // Yeni ürün eklendi
    UPDATED,  // Mevcut ürün güncellendi
    SKIPPED   // Değişiklik yok, atllandı
}
