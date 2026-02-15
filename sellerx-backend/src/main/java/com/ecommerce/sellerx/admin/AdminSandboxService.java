package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.CreateSandboxInvoiceRequest;
import com.ecommerce.sellerx.admin.dto.CreateSandboxOrderRequest;
import com.ecommerce.sellerx.admin.dto.CreateSandboxProductRequest;
import com.ecommerce.sellerx.admin.dto.CreateSandboxReturnRequest;
import com.ecommerce.sellerx.admin.dto.SandboxInvoiceDto;
import com.ecommerce.sellerx.financial.TrendyolDeductionInvoice;
import com.ecommerce.sellerx.financial.TrendyolDeductionInvoiceRepository;
import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.returns.ReturnRecord;
import com.ecommerce.sellerx.returns.ReturnRecordRepository;
import com.ecommerce.sellerx.orders.OrderItem;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderDto;
import com.ecommerce.sellerx.orders.TrendyolOrderMapper;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import com.ecommerce.sellerx.orders.OrderCostCalculator;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductDto;
import com.ecommerce.sellerx.products.TrendyolProductMapper;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSandboxService {

    // Sandbox mağazası ID (Furkan Test Mağaza)
    private static final UUID SANDBOX_STORE_ID =
            UUID.fromString("b7902a53-3e8f-47e2-b6ff-7e654dbfcbd5");

    private final TrendyolProductRepository productRepository;
    private final TrendyolOrderRepository orderRepository;
    private final TrendyolDeductionInvoiceRepository invoiceRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final StoreRepository storeRepository;
    private final TrendyolProductMapper productMapper;
    private final TrendyolOrderMapper orderMapper;
    private final OrderCostCalculator orderCostCalculator;

    // Kargo birim fiyatı (TL/desi)
    private static final BigDecimal CARGO_UNIT_PRICE = BigDecimal.valueOf(8);
    // Stopaj oranı (%1)
    private static final BigDecimal STOPPAGE_RATE = BigDecimal.valueOf(0.01);

    /**
     * Sandbox mağazasının tüm ürünlerini listeler
     */
    public List<TrendyolProductDto> getSandboxProducts() {
        List<TrendyolProduct> products = productRepository.findByStoreId(SANDBOX_STORE_ID);
        return products.stream()
                .map(productMapper::toDto)
                .toList();
    }

    /**
     * Sandbox mağazasına yeni ürün ekler
     */
    @Transactional
    public TrendyolProductDto createSandboxProduct(CreateSandboxProductRequest request) {
        Store store = storeRepository.findById(SANDBOX_STORE_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Sandbox store not found"));

        // Benzersiz productId oluştur (SANDBOX_ prefix ile)
        String productId = "SANDBOX_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        TrendyolProduct product = TrendyolProduct.builder()
                .store(store)
                .productId(productId)
                .barcode(request.getBarcode())
                .title(request.getTitle())
                .brand(request.getBrand())
                .brandId(request.getBrandId())
                .categoryName(request.getCategoryName())
                .pimCategoryId(request.getPimCategoryId())
                .salePrice(request.getSalePrice())
                .vatRate(request.getVatRate())
                .trendyolQuantity(request.getQuantity() != null ? request.getQuantity() : 0)
                .productMainId(request.getProductMainId())
                .dimensionalWeight(request.getDimensionalWeight())
                .image(request.getImage())
                .productUrl(request.getProductUrl())
                .approved(request.getApproved() != null ? request.getApproved() : true)
                .onSale(request.getOnSale() != null ? request.getOnSale() : true)
                .archived(request.getArchived() != null ? request.getArchived() : false)
                .rejected(request.getRejected() != null ? request.getRejected() : false)
                .blacklisted(request.getBlacklisted() != null ? request.getBlacklisted() : false)
                .hasActiveCampaign(request.getHasActiveCampaign() != null ? request.getHasActiveCampaign() : false)
                .costAndStockInfo(new ArrayList<>())
                .createDateTime(System.currentTimeMillis())
                .build();

        TrendyolProduct savedProduct = productRepository.save(product);
        log.info("[SANDBOX] Yeni ürün eklendi: {} - {}", savedProduct.getBarcode(), savedProduct.getTitle());

        return productMapper.toDto(savedProduct);
    }

    /**
     * Sandbox ürününü günceller
     */
    @Transactional
    public TrendyolProductDto updateSandboxProduct(UUID productId, CreateSandboxProductRequest request) {
        TrendyolProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));

        // Sadece sandbox mağazasına ait ürünler güncellenebilir
        if (!product.getStore().getId().equals(SANDBOX_STORE_ID)) {
            throw new IllegalArgumentException("Bu ürün sandbox mağazasına ait değil!");
        }

        // Alanları güncelle
        if (request.getBarcode() != null) product.setBarcode(request.getBarcode());
        if (request.getTitle() != null) product.setTitle(request.getTitle());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getBrandId() != null) product.setBrandId(request.getBrandId());
        if (request.getCategoryName() != null) product.setCategoryName(request.getCategoryName());
        if (request.getPimCategoryId() != null) product.setPimCategoryId(request.getPimCategoryId());
        if (request.getSalePrice() != null) product.setSalePrice(request.getSalePrice());
        if (request.getVatRate() != null) product.setVatRate(request.getVatRate());
        if (request.getQuantity() != null) product.setTrendyolQuantity(request.getQuantity());
        if (request.getProductMainId() != null) product.setProductMainId(request.getProductMainId());
        if (request.getDimensionalWeight() != null) product.setDimensionalWeight(request.getDimensionalWeight());
        if (request.getImage() != null) product.setImage(request.getImage());
        if (request.getProductUrl() != null) product.setProductUrl(request.getProductUrl());
        if (request.getApproved() != null) product.setApproved(request.getApproved());
        if (request.getOnSale() != null) product.setOnSale(request.getOnSale());
        if (request.getArchived() != null) product.setArchived(request.getArchived());

        TrendyolProduct savedProduct = productRepository.save(product);
        log.info("[SANDBOX] Ürün güncellendi: {} - {}", savedProduct.getBarcode(), savedProduct.getTitle());

        return productMapper.toDto(savedProduct);
    }

    /**
     * Sandbox ürününü siler
     */
    @Transactional
    public void deleteSandboxProduct(UUID productId) {
        TrendyolProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));

        // Sadece sandbox mağazasına ait ürünler silinebilir
        if (!product.getStore().getId().equals(SANDBOX_STORE_ID)) {
            throw new IllegalArgumentException("Bu ürün sandbox mağazasına ait değil!");
        }

        productRepository.delete(product);
        log.info("[SANDBOX] Ürün silindi: {} - {}", product.getBarcode(), product.getTitle());
    }

    /**
     * Sandbox mağazasının ID'sini döner
     */
    public UUID getSandboxStoreId() {
        return SANDBOX_STORE_ID;
    }

    // ============================================
    // SANDBOX ORDER METHODS
    // ============================================

    /**
     * Sandbox mağazasının tüm siparişlerini listeler
     */
    public List<TrendyolOrderDto> getSandboxOrders() {
        List<TrendyolOrder> orders = orderRepository.findByStoreIdOrderByOrderDateDesc(
                SANDBOX_STORE_ID,
                org.springframework.data.domain.PageRequest.of(0, 100)
        ).getContent();
        return orders.stream()
                .map(orderMapper::toDto)
                .toList();
    }

    /**
     * Sandbox mağazasına yeni sipariş ekler.
     *
     * CANLI SİSTEMLE %100 UYUMLU ALGORİTMA:
     * - Komisyon: product.lastCommissionRate → product.commissionRate → 0
     * - Kargo: product.lastShippingCostPerUnit → 0
     *
     * commissionSource değerleri:
     * - INVOICE: lastCommissionRate var (faturadan)
     * - REFERENCE: sadece commissionRate var (ürün referansından)
     * - NONE: hiçbir veri yok
     */
    @Transactional
    public TrendyolOrderDto createSandboxOrder(CreateSandboxOrderRequest request) {
        Store store = storeRepository.findById(SANDBOX_STORE_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Sandbox store not found"));

        // Ürünü barkod ile bul
        TrendyolProduct product = productRepository.findByStoreIdAndBarcode(SANDBOX_STORE_ID, request.getBarcode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sandbox ürünü bulunamadı: " + request.getBarcode() + ". Önce ürün eklemeniz gerekiyor."));

        // Benzersiz order numarası oluştur
        String orderNumber = "SANDBOX-" + System.currentTimeMillis();
        Long packageNo = System.currentTimeMillis();

        // Sipariş tarihi (default: bugün)
        LocalDate orderDate = request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now();
        LocalDateTime orderDateTime = orderDate.atTime(12, 0);

        // Quantity ve unit price
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : product.getSalePrice();
        BigDecimal grossAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // === CANLI SİSTEM ALGORİTMASI: Komisyon ===
        // Priority: lastCommissionRate (faturadan) → commissionRate (ürün API'den) → 0
        BigDecimal commissionRate = orderCostCalculator.getEffectiveCommissionRate(product);
        String commissionSource = determineCommissionSource(product);

        // Komisyon hesapla (KDV dahil fiyat üzerinden, canlı sistemle aynı)
        BigDecimal vatRate = product.getVatRate() != null ? BigDecimal.valueOf(product.getVatRate()) : BigDecimal.valueOf(20);
        BigDecimal estimatedCommission = orderCostCalculator.calculateUnitEstimatedCommission(
                grossAmount, vatRate, commissionRate);

        // === CANLI SİSTEM ALGORİTMASI: Kargo ===
        // Priority: lastShippingCostPerUnit (kargo faturasından) → 0
        BigDecimal shippingCostPerUnit = orderCostCalculator.getEffectiveShippingCostPerUnit(product);
        BigDecimal shippingCost = shippingCostPerUnit.multiply(BigDecimal.valueOf(quantity));

        // Stopaj hesapla (%1)
        BigDecimal stoppage = grossAmount.multiply(STOPPAGE_RATE).setScale(2, RoundingMode.HALF_UP);

        // OrderItem oluştur
        OrderItem orderItem = OrderItem.builder()
                .barcode(request.getBarcode())
                .productName(product.getTitle())
                .quantity(quantity)
                .unitPriceOrder(unitPrice)
                .unitPriceDiscount(BigDecimal.ZERO)
                .unitPriceTyDiscount(BigDecimal.ZERO)
                .vatBaseAmount(grossAmount)
                .price(grossAmount)
                .estimatedCommissionRate(commissionRate)
                .unitEstimatedCommission(estimatedCommission.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP))
                .cost(getProductLatestCost(product)) // Ürünün gerçek maliyeti (costAndStockInfo'dan)
                .build();

        // Müşteri bilgileri (opsiyonel)
        String customerFirstName = null;
        String customerLastName = null;
        if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            String[] nameParts = request.getCustomerName().split(" ", 2);
            customerFirstName = nameParts[0];
            customerLastName = nameParts.length > 1 ? nameParts[1] : "";
        }

        // isCommissionEstimated: Sadece veri varsa tahmini, yoksa false (gösterilmeyecek)
        // NONE → false (veri yok, tahmin edilecek bir şey yok)
        // REFERENCE/INVOICE → true (veri var, tahmini hesaplandı, settle bekliyor)
        boolean isEstimated = !"NONE".equals(commissionSource);

        // TrendyolOrder oluştur
        TrendyolOrder order = TrendyolOrder.builder()
                .store(store)
                .tyOrderNumber(orderNumber)
                .packageNo(packageNo)
                .orderDate(orderDateTime)
                .grossAmount(grossAmount)
                .totalDiscount(BigDecimal.ZERO)
                .totalTyDiscount(BigDecimal.ZERO)
                .totalPrice(grossAmount)
                .stoppage(stoppage)
                .estimatedCommission(estimatedCommission)
                .isCommissionEstimated(isEstimated) // NONE ise false, diğerleri true
                .estimatedShippingCost(shippingCost)
                .isShippingEstimated(shippingCostPerUnit.compareTo(BigDecimal.ZERO) == 0) // Veri yoksa tahmini
                .commissionSource(commissionSource)
                .orderItems(List.of(orderItem))
                .status(request.getStatus() != null ? request.getStatus() : "Delivered")
                .shipmentPackageStatus("Delivered")
                .shipmentCity(request.getCity())
                .customerFirstName(customerFirstName)
                .customerLastName(customerLastName)
                .dataSource("SANDBOX")
                .cargoDeci(product.getDimensionalWeight() != null ? product.getDimensionalWeight().intValue() : 1)
                .deliveryDate(orderDateTime.plusDays(2))
                .build();

        TrendyolOrder savedOrder = orderRepository.save(order);
        log.info("[SANDBOX] Yeni sipariş eklendi: {} - {} adet {} - Komisyon: {} ({}) - Kargo: {}",
                savedOrder.getTyOrderNumber(), quantity, product.getTitle(),
                estimatedCommission, commissionSource, shippingCost);

        return orderMapper.toDto(savedOrder);
    }

    /**
     * Ürünün komisyon kaynağını belirler.
     * INVOICE: lastCommissionRate var (faturadan gelen değer)
     * REFERENCE: sadece commissionRate var (ürün API'den gelen kategori default)
     * NONE: hiçbir komisyon verisi yok
     */
    private String determineCommissionSource(TrendyolProduct product) {
        if (product.getLastCommissionRate() != null) {
            return "INVOICE";
        } else if (product.getCommissionRate() != null) {
            return "REFERENCE";
        }
        return "NONE";
    }

    /**
     * Sandbox siparişini günceller.
     * Canlı sistemle aynı algoritma: Komisyon ve kargo referans değerlerden alınır.
     */
    @Transactional
    public TrendyolOrderDto updateSandboxOrder(UUID orderId, CreateSandboxOrderRequest request) {
        TrendyolOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        // Sadece sandbox mağazasına ait siparişler güncellenebilir
        if (!order.getStore().getId().equals(SANDBOX_STORE_ID)) {
            throw new IllegalArgumentException("Bu sipariş sandbox mağazasına ait değil!");
        }

        // Ürünü barkod ile bul
        TrendyolProduct product = productRepository.findByStoreIdAndBarcode(SANDBOX_STORE_ID, request.getBarcode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sandbox ürünü bulunamadı: " + request.getBarcode()));

        // Sipariş tarihi
        if (request.getOrderDate() != null) {
            order.setOrderDate(request.getOrderDate().atTime(12, 0));
            order.setDeliveryDate(request.getOrderDate().atTime(12, 0).plusDays(2));
        }

        // Quantity ve unit price
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : product.getSalePrice();
        BigDecimal grossAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // === CANLI SİSTEM ALGORİTMASI ===
        BigDecimal commissionRate = orderCostCalculator.getEffectiveCommissionRate(product);
        String commissionSource = determineCommissionSource(product);

        BigDecimal vatRate = product.getVatRate() != null ? BigDecimal.valueOf(product.getVatRate()) : BigDecimal.valueOf(20);
        BigDecimal estimatedCommission = orderCostCalculator.calculateUnitEstimatedCommission(
                grossAmount, vatRate, commissionRate);

        BigDecimal shippingCostPerUnit = orderCostCalculator.getEffectiveShippingCostPerUnit(product);
        BigDecimal shippingCost = shippingCostPerUnit.multiply(BigDecimal.valueOf(quantity));

        // Stopaj hesapla (%1)
        BigDecimal stoppage = grossAmount.multiply(STOPPAGE_RATE).setScale(2, RoundingMode.HALF_UP);

        // OrderItem güncelle
        OrderItem orderItem = OrderItem.builder()
                .barcode(request.getBarcode())
                .productName(product.getTitle())
                .quantity(quantity)
                .unitPriceOrder(unitPrice)
                .unitPriceDiscount(BigDecimal.ZERO)
                .unitPriceTyDiscount(BigDecimal.ZERO)
                .vatBaseAmount(grossAmount)
                .price(grossAmount)
                .estimatedCommissionRate(commissionRate)
                .unitEstimatedCommission(estimatedCommission.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP))
                .cost(getProductLatestCost(product))
                .build();

        // isCommissionEstimated: NONE ise false, diğerleri true (settle bekliyor)
        boolean isEstimated = !"NONE".equals(commissionSource);

        // Order güncelle
        order.setGrossAmount(grossAmount);
        order.setTotalPrice(grossAmount);
        order.setStoppage(stoppage);
        order.setEstimatedCommission(estimatedCommission);
        order.setEstimatedShippingCost(shippingCost);
        order.setIsShippingEstimated(shippingCostPerUnit.compareTo(BigDecimal.ZERO) == 0);
        order.setCommissionSource(commissionSource);
        order.setIsCommissionEstimated(isEstimated); // NONE ise false
        order.setOrderItems(List.of(orderItem));
        if (request.getStatus() != null) {
            order.setStatus(request.getStatus());
            order.setShipmentPackageStatus(request.getStatus());
        }
        if (request.getCity() != null) order.setShipmentCity(request.getCity());

        // Müşteri bilgileri
        if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            String[] nameParts = request.getCustomerName().split(" ", 2);
            order.setCustomerFirstName(nameParts[0]);
            order.setCustomerLastName(nameParts.length > 1 ? nameParts[1] : "");
        }

        TrendyolOrder savedOrder = orderRepository.save(order);
        log.info("[SANDBOX] Sipariş güncellendi: {} - {} adet {} - Komisyon: {} ({})",
                savedOrder.getTyOrderNumber(), quantity, product.getTitle(), estimatedCommission, commissionSource);

        return orderMapper.toDto(savedOrder);
    }

    /**
     * Sandbox siparişini siler
     */
    @Transactional
    public void deleteSandboxOrder(UUID orderId) {
        TrendyolOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        // Sadece sandbox mağazasına ait siparişler silinebilir
        if (!order.getStore().getId().equals(SANDBOX_STORE_ID)) {
            throw new IllegalArgumentException("Bu sipariş sandbox mağazasına ait değil!");
        }

        orderRepository.delete(order);
        log.info("[SANDBOX] Sipariş silindi: {}", order.getTyOrderNumber());
    }

    /**
     * Ürünün en güncel maliyetini costAndStockInfo listesinden alır.
     * Eğer maliyet girilmemişse null döner.
     */
    private BigDecimal getProductLatestCost(TrendyolProduct product) {
        if (product.getCostAndStockInfo() == null || product.getCostAndStockInfo().isEmpty()) {
            return null;
        }

        // En son eklenen maliyet kaydını al (listenin sonundaki)
        CostAndStockInfo latestCost = product.getCostAndStockInfo()
                .get(product.getCostAndStockInfo().size() - 1);

        Double effectiveCost = latestCost.getEffectiveUnitCost();
        if (effectiveCost == null || effectiveCost <= 0) {
            return null;
        }

        return BigDecimal.valueOf(effectiveCost);
    }

    // ============================================
    // SANDBOX INVOICE METHODS
    // ============================================

    /**
     * Sandbox mağazasının tüm faturalarını listeler
     */
    public List<SandboxInvoiceDto> getSandboxInvoices() {
        // Son 1 yıllık faturaları getir
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusYears(1);

        List<TrendyolDeductionInvoice> invoices = invoiceRepository
                .findByStoreIdAndTransactionDateBetween(SANDBOX_STORE_ID, startDate, endDate);

        return invoices.stream()
                .map(this::mapToDto)
                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                .toList();
    }

    /**
     * Sandbox mağazasına yeni fatura ekler.
     *
     * CANLI SİSTEMLE %100 UYUMLU:
     * - Komisyon Faturası eklenirse → product.lastCommissionRate güncellenir
     * - Kargo Fatura eklenirse → product.lastShippingCostPerUnit güncellenir
     *
     * Bu sayede sonraki siparişler bu değerleri referans alır.
     */
    @Transactional
    public SandboxInvoiceDto createSandboxInvoice(CreateSandboxInvoiceRequest request) {
        // Benzersiz trendyolId oluştur (SANDBOX_ prefix ile)
        String trendyolId = "SANDBOX_INV_" + System.currentTimeMillis();

        // Fatura tarihi (default: bugün)
        LocalDateTime transactionDate = request.getTransactionDate() != null
                ? request.getTransactionDate().atStartOfDay()
                : LocalDateTime.now();

        // Fatura seri numarası
        String invoiceSerialNumber = request.getInvoiceSerialNumber() != null
                ? request.getInvoiceSerialNumber()
                : "SANDBOX-" + System.currentTimeMillis();

        // Tutar: işlem tipi "IADE" içeriyorsa credit, değilse debt
        BigDecimal debt = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;

        // Check if this is a refund type transaction (contains "IADE")
        boolean isRefundType = request.getTransactionType() != null &&
                request.getTransactionType().toUpperCase(java.util.Locale.ENGLISH).contains("IADE");

        if (request.getAmount() != null) {
            BigDecimal absoluteAmount = request.getAmount().abs();
            if (isRefundType) {
                // Refund types (IADE) always go to credit field
                credit = absoluteAmount;
            } else if (request.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
                // Normal types: positive = debt
                debt = request.getAmount();
            } else {
                // Normal types: negative = credit (manual override)
                credit = absoluteAmount;
            }
        }

        TrendyolDeductionInvoice invoice = TrendyolDeductionInvoice.builder()
                .storeId(SANDBOX_STORE_ID)
                .trendyolId(trendyolId)
                .transactionDate(transactionDate)
                .transactionType(request.getTransactionType())
                .description(request.getDescription())
                .debt(debt)
                .credit(credit)
                .invoiceSerialNumber(invoiceSerialNumber)
                .orderNumber(request.getOrderNumber())
                .productBarcode(request.getBarcode()) // Fatura silindiğinde ürünü bulabilmek için
                .currency("TRY")
                .createdAt(LocalDateTime.now())
                .build();

        TrendyolDeductionInvoice savedInvoice = invoiceRepository.save(invoice);

        // === ÜRÜN GÜNCELLEME (CANLI SİSTEMLE AYNI) ===
        // Komisyon veya Kargo faturası ise ilgili ürünü güncelle
        updateProductFromInvoice(request);

        log.info("[SANDBOX] Yeni fatura eklendi: {} - {} - Tutar: {}",
                savedInvoice.getTransactionType(),
                savedInvoice.getInvoiceSerialNumber(),
                debt.compareTo(BigDecimal.ZERO) > 0 ? debt : credit.negate());

        return mapToDto(savedInvoice);
    }

    /**
     * Komisyon veya Kargo faturası eklendiğinde ilgili ürünü günceller.
     * Bu, canlı sistemdeki TrendyolInvoiceService davranışını simüle eder.
     */
    private void updateProductFromInvoice(CreateSandboxInvoiceRequest request) {
        if (request.getBarcode() == null || request.getBarcode().isBlank()) {
            return; // Ürün bağlantısı yoksa güncelleme yapma
        }

        TrendyolProduct product = productRepository.findByStoreIdAndBarcode(SANDBOX_STORE_ID, request.getBarcode())
                .orElse(null);

        if (product == null) {
            log.warn("[SANDBOX] Fatura için ürün bulunamadı: {}", request.getBarcode());
            return;
        }

        boolean updated = false;

        // Komisyon Faturası: lastCommissionRate güncelle
        if ("Komisyon Faturası".equals(request.getTransactionType()) && request.getCommissionRate() != null) {
            product.setLastCommissionRate(request.getCommissionRate());
            updated = true;
            log.info("[SANDBOX] Ürün komisyon oranı güncellendi: {} → %{}",
                    product.getBarcode(), request.getCommissionRate());
        }

        // Kargo Fatura: lastShippingCostPerUnit güncelle
        if ("Kargo Fatura".equals(request.getTransactionType()) && request.getShippingCostPerUnit() != null) {
            product.setLastShippingCostPerUnit(request.getShippingCostPerUnit());
            product.setLastShippingCostDate(LocalDateTime.now());
            updated = true;
            log.info("[SANDBOX] Ürün kargo maliyeti güncellendi: {} → {} TL/adet",
                    product.getBarcode(), request.getShippingCostPerUnit());
        }

        if (updated) {
            productRepository.save(product);
        }
    }

    /**
     * Sandbox faturasını günceller
     */
    @Transactional
    public SandboxInvoiceDto updateSandboxInvoice(UUID invoiceId, CreateSandboxInvoiceRequest request) {
        TrendyolDeductionInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId.toString()));

        // Sadece sandbox mağazasına ait faturalar güncellenebilir
        if (!invoice.getStoreId().equals(SANDBOX_STORE_ID)) {
            throw new IllegalArgumentException("Bu fatura sandbox mağazasına ait değil!");
        }

        // Alanları güncelle
        if (request.getTransactionType() != null) {
            invoice.setTransactionType(request.getTransactionType());
        }
        if (request.getTransactionDate() != null) {
            invoice.setTransactionDate(request.getTransactionDate().atStartOfDay());
        }
        if (request.getDescription() != null) {
            invoice.setDescription(request.getDescription());
        }
        if (request.getOrderNumber() != null) {
            invoice.setOrderNumber(request.getOrderNumber());
        }
        if (request.getInvoiceSerialNumber() != null) {
            invoice.setInvoiceSerialNumber(request.getInvoiceSerialNumber());
        }
        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
                invoice.setDebt(request.getAmount());
                invoice.setCredit(BigDecimal.ZERO);
            } else {
                invoice.setDebt(BigDecimal.ZERO);
                invoice.setCredit(request.getAmount().abs());
            }
        }

        TrendyolDeductionInvoice savedInvoice = invoiceRepository.save(invoice);
        log.info("[SANDBOX] Fatura güncellendi: {} - {}",
                savedInvoice.getTransactionType(), savedInvoice.getInvoiceSerialNumber());

        return mapToDto(savedInvoice);
    }

    /**
     * Sandbox faturasını siler.
     *
     * CANLI SİSTEMLE %100 UYUMLU:
     * - Komisyon Faturası silinirse → product.lastCommissionRate = null
     * - Kargo Fatura silinirse → product.lastShippingCostPerUnit = null
     *
     * Bu sayede sonraki siparişler doğru kaynak bilgisini gösterir.
     */
    @Transactional
    public void deleteSandboxInvoice(UUID invoiceId) {
        TrendyolDeductionInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId.toString()));

        // Sadece sandbox mağazasına ait faturalar silinebilir
        if (!invoice.getStoreId().equals(SANDBOX_STORE_ID)) {
            throw new IllegalArgumentException("Bu fatura sandbox mağazasına ait değil!");
        }

        // === ÜRÜN DEĞERLERİNİ SIFIRLA (CANLI SİSTEMLE AYNI) ===
        // Fatura silindiğinde ilgili ürünün komisyon/kargo değerlerini sıfırla
        resetProductFromDeletedInvoice(invoice);

        invoiceRepository.delete(invoice);
        log.info("[SANDBOX] Fatura silindi: {} - {}",
                invoice.getTransactionType(), invoice.getInvoiceSerialNumber());
    }

    /**
     * Fatura silindiğinde ilgili ürünün komisyon/kargo değerlerini sıfırlar.
     * Bu sayede sonraki siparişler "NONE" kaynağını gösterir.
     */
    private void resetProductFromDeletedInvoice(TrendyolDeductionInvoice invoice) {
        String barcode = invoice.getProductBarcode();
        if (barcode == null || barcode.isBlank()) {
            return; // Ürün bağlantısı yoksa sıfırlama yapma
        }

        TrendyolProduct product = productRepository.findByStoreIdAndBarcode(SANDBOX_STORE_ID, barcode)
                .orElse(null);

        if (product == null) {
            log.warn("[SANDBOX] Silinen fatura için ürün bulunamadı: {}", barcode);
            return;
        }

        boolean updated = false;

        // Komisyon Faturası silinirse: lastCommissionRate = null
        if (TrendyolDeductionInvoice.TYPE_COMMISSION_INVOICE.equals(invoice.getTransactionType())) {
            product.setLastCommissionRate(null);
            updated = true;
            log.info("[SANDBOX] Ürün komisyon oranı sıfırlandı: {} (fatura silindi)",
                    product.getBarcode());
        }

        // Kargo Fatura silinirse: lastShippingCostPerUnit = null
        if (TrendyolDeductionInvoice.TYPE_CARGO_INVOICE.equals(invoice.getTransactionType())) {
            product.setLastShippingCostPerUnit(null);
            product.setLastShippingCostDate(null);
            updated = true;
            log.info("[SANDBOX] Ürün kargo maliyeti sıfırlandı: {} (fatura silindi)",
                    product.getBarcode());
        }

        if (updated) {
            productRepository.save(product);
        }
    }

    /**
     * Entity'yi DTO'ya dönüştürür
     */
    private SandboxInvoiceDto mapToDto(TrendyolDeductionInvoice invoice) {
        return SandboxInvoiceDto.builder()
                .id(invoice.getId())
                .trendyolId(invoice.getTrendyolId())
                .transactionDate(invoice.getTransactionDate())
                .transactionType(invoice.getTransactionType())
                .description(invoice.getDescription())
                .debt(invoice.getDebt())
                .credit(invoice.getCredit())
                .invoiceSerialNumber(invoice.getInvoiceSerialNumber())
                .orderNumber(invoice.getOrderNumber())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    // ============================================
    // SANDBOX RETURN METHODS
    // ============================================

    /**
     * Sandbox mağazasının tüm iadelerini listeler
     */
    public List<ReturnRecord> getSandboxReturns() {
        return returnRecordRepository.findByStoreId(SANDBOX_STORE_ID);
    }

    /**
     * Sandbox mağazasına yeni iade ekler
     */
    @Transactional
    public ReturnRecord createSandboxReturn(CreateSandboxReturnRequest request) {
        Store store = storeRepository.findById(SANDBOX_STORE_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Sandbox store not found"));

        // Siparişi bul
        TrendyolOrder order = orderRepository.findByStoreIdAndTyOrderNumber(SANDBOX_STORE_ID, request.getOrderNumber())
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sandbox siparişi bulunamadı: " + request.getOrderNumber()));

        // Ürünü bul (ürün adı ve maliyet için)
        TrendyolProduct product = productRepository.findByStoreIdAndBarcode(SANDBOX_STORE_ID, request.getBarcode())
                .orElse(null);

        // Ürün adı: request'ten veya siparişten veya üründen
        String productName = request.getProductName();
        if (productName == null && order.getOrderItems() != null) {
            productName = order.getOrderItems().stream()
                    .filter(item -> request.getBarcode().equals(item.getBarcode()))
                    .map(OrderItem::getProductName)
                    .findFirst()
                    .orElse(product != null ? product.getTitle() : request.getBarcode());
        }

        // Ürün maliyeti: request'ten veya FIFO'dan
        BigDecimal productCost = request.getProductCost();
        if (productCost == null && product != null) {
            productCost = getProductLatestCost(product);
        }
        if (productCost == null) {
            productCost = BigDecimal.ZERO;
        }

        // Komisyon kaybı: request'ten veya siparişten hesapla
        BigDecimal commissionLoss = request.getCommissionLoss();
        if (commissionLoss == null && order.getEstimatedCommission() != null) {
            // Siparişteki toplam komisyonu iade miktarına göre böl
            int orderQuantity = order.getOrderItems() != null
                    ? order.getOrderItems().stream().mapToInt(OrderItem::getQuantity).sum()
                    : 1;
            int returnQuantity = request.getQuantity() != null ? request.getQuantity() : 1;
            commissionLoss = order.getEstimatedCommission()
                    .multiply(BigDecimal.valueOf(returnQuantity))
                    .divide(BigDecimal.valueOf(orderQuantity), 2, RoundingMode.HALF_UP);
        }
        if (commissionLoss == null) {
            commissionLoss = BigDecimal.ZERO;
        }

        // İade tarihi
        LocalDate returnDate = request.getReturnDate() != null ? request.getReturnDate() : LocalDate.now();

        ReturnRecord returnRecord = ReturnRecord.builder()
                .store(store)
                .order(order)
                .barcode(request.getBarcode())
                .productName(productName)
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .productCost(productCost.multiply(BigDecimal.valueOf(request.getQuantity() != null ? request.getQuantity() : 1)))
                .shippingCostOut(request.getShippingCostOut() != null ? request.getShippingCostOut() : BigDecimal.valueOf(25))
                .shippingCostReturn(request.getShippingCostReturn() != null ? request.getShippingCostReturn() : BigDecimal.valueOf(25))
                .commissionLoss(commissionLoss)
                .packagingCost(request.getPackagingCost() != null ? request.getPackagingCost() : BigDecimal.valueOf(5))
                .returnReason(request.getReturnReason())
                .returnDate(returnDate.atStartOfDay())
                .returnStatus("RECEIVED")
                .commissionRefunded(false)
                .build();

        // Toplam zararı hesapla
        returnRecord.calculateTotalLoss();

        ReturnRecord savedReturn = returnRecordRepository.save(returnRecord);
        log.info("[SANDBOX] Yeni iade eklendi: {} - {} - Toplam Zarar: {}",
                savedReturn.getBarcode(), savedReturn.getProductName(), savedReturn.getTotalLoss());

        return savedReturn;
    }

    /**
     * Sandbox iadesini siler
     */
    @Transactional
    public void deleteSandboxReturn(UUID returnId) {
        ReturnRecord returnRecord = returnRecordRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return", returnId.toString()));

        // Sadece sandbox mağazasına ait iadeler silinebilir
        if (!returnRecord.getStore().getId().equals(SANDBOX_STORE_ID)) {
            throw new IllegalArgumentException("Bu iade sandbox mağazasına ait değil!");
        }

        returnRecordRepository.delete(returnRecord);
        log.info("[SANDBOX] İade silindi: {} - {}", returnRecord.getBarcode(), returnRecord.getProductName());
    }

    // ============================================
    // SANDBOX SETTLEMENT METHODS
    // ============================================

    /**
     * Tek bir sandbox siparişini "settle" eder (komisyonu gerçek yapar)
     * isCommissionEstimated: true → false
     * transactionStatus: SETTLED
     */
    @Transactional
    public TrendyolOrderDto settleSandboxOrder(UUID orderId) {
        TrendyolOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        // Sadece sandbox mağazasına ait siparişler settle edilebilir
        if (!order.getStore().getId().equals(SANDBOX_STORE_ID)) {
            throw new IllegalArgumentException("Bu sipariş sandbox mağazasına ait değil!");
        }

        // Zaten settle edilmişse uyar
        if (!order.getIsCommissionEstimated()) {
            log.warn("[SANDBOX] Sipariş zaten settle edilmiş: {}", order.getTyOrderNumber());
            return orderMapper.toDto(order);
        }

        order.setIsCommissionEstimated(false);
        order.setTransactionStatus("SETTLED");
        order.setIsShippingEstimated(false); // Kargo da gerçek oldu

        TrendyolOrder savedOrder = orderRepository.save(order);
        log.info("[SANDBOX] Sipariş settle edildi: {} - Komisyon: {}",
                savedOrder.getTyOrderNumber(), savedOrder.getEstimatedCommission());

        return orderMapper.toDto(savedOrder);
    }

    /**
     * Tüm sandbox siparişlerini settle eder (NONE hariç - veri yoksa settle edilecek bir şey yok)
     * @return Settle edilen sipariş sayısı
     */
    @Transactional
    public int settleAllSandboxOrders() {
        List<TrendyolOrder> unsettledOrders = orderRepository.findByStoreIdAndIsCommissionEstimatedTrue(
                SANDBOX_STORE_ID);

        // NONE olanları filtrele - veri yoksa settle edilecek bir şey yok
        List<TrendyolOrder> ordersToSettle = unsettledOrders.stream()
                .filter(o -> !"NONE".equals(o.getCommissionSource()))
                .toList();

        if (ordersToSettle.isEmpty()) {
            log.info("[SANDBOX] Settle edilecek sipariş bulunamadı");
            return 0;
        }

        for (TrendyolOrder order : ordersToSettle) {
            order.setIsCommissionEstimated(false);
            order.setTransactionStatus("SETTLED");
            order.setIsShippingEstimated(false);
        }

        orderRepository.saveAll(ordersToSettle);
        log.info("[SANDBOX] {} adet sipariş settle edildi", ordersToSettle.size());

        return ordersToSettle.size();
    }
}
