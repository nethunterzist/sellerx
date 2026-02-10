package com.ecommerce.sellerx.purchasing;

import com.ecommerce.sellerx.orders.StockOrderSynchronizationService;
import com.ecommerce.sellerx.products.CostAndStockInfo;
import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final TrendyolProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StockOrderSynchronizationService stockSyncService;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderAttachmentRepository attachmentRepository;

    // === List & Get ===

    public List<PurchaseOrderSummaryDto> getPurchaseOrders(UUID storeId, PurchaseOrderStatus status,
                                                            LocalDate startDate, LocalDate endDate) {
        List<PurchaseOrder> orders;

        if (startDate != null && endDate != null) {
            if (status != null) {
                orders = purchaseOrderRepository.findByStoreIdAndPoDateBetweenAndStatus(
                        storeId, startDate, endDate, status);
            } else {
                orders = purchaseOrderRepository.findByStoreIdAndPoDateBetween(
                        storeId, startDate, endDate);
            }
        } else if (status != null) {
            orders = purchaseOrderRepository.findByStoreIdAndStatusOrderByPoDateDesc(storeId, status);
        } else {
            orders = purchaseOrderRepository.findByStoreIdOrderByPoDateDesc(storeId);
        }

        return orders.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    public PurchaseOrderDto getPurchaseOrder(UUID storeId, Long poId) {
        PurchaseOrder po = purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));
        return toDto(po);
    }

    public PurchaseOrderStatsDto getStats(UUID storeId) {
        return PurchaseOrderStatsDto.builder()
                .draft(getStatusStats(storeId, PurchaseOrderStatus.DRAFT))
                .ordered(getStatusStats(storeId, PurchaseOrderStatus.ORDERED))
                .shipped(getStatusStats(storeId, PurchaseOrderStatus.SHIPPED))
                .closed(getStatusStats(storeId, PurchaseOrderStatus.CLOSED))
                .build();
    }

    public PurchaseOrderStatsDto getStats(UUID storeId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return getStats(storeId);
        }
        return PurchaseOrderStatsDto.builder()
                .draft(getStatusStats(storeId, PurchaseOrderStatus.DRAFT, startDate, endDate))
                .ordered(getStatusStats(storeId, PurchaseOrderStatus.ORDERED, startDate, endDate))
                .shipped(getStatusStats(storeId, PurchaseOrderStatus.SHIPPED, startDate, endDate))
                .closed(getStatusStats(storeId, PurchaseOrderStatus.CLOSED, startDate, endDate))
                .build();
    }

    private PurchaseOrderStatsDto.StatusStats getStatusStats(UUID storeId, PurchaseOrderStatus status) {
        return PurchaseOrderStatsDto.StatusStats.builder()
                .count(purchaseOrderRepository.countByStoreIdAndStatus(storeId, status))
                .totalCost(purchaseOrderRepository.sumTotalCostByStoreIdAndStatus(storeId, status))
                .totalUnits(purchaseOrderRepository.sumTotalUnitsByStoreIdAndStatus(storeId, status))
                .build();
    }

    private PurchaseOrderStatsDto.StatusStats getStatusStats(UUID storeId, PurchaseOrderStatus status,
                                                              LocalDate startDate, LocalDate endDate) {
        return PurchaseOrderStatsDto.StatusStats.builder()
                .count(purchaseOrderRepository.countByStoreIdAndStatusAndDateRange(storeId, status, startDate, endDate))
                .totalCost(purchaseOrderRepository.sumTotalCostByStoreIdAndStatusAndDateRange(storeId, status, startDate, endDate))
                .totalUnits(purchaseOrderRepository.sumTotalUnitsByStoreIdAndStatusAndDateRange(storeId, status, startDate, endDate))
                .build();
    }

    // === CRUD ===

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(UUID storeId, CreatePurchaseOrderRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        // Generate PO number
        Long count = purchaseOrderRepository.countByStoreId(storeId);
        String poNumber = String.format("PO-%06d", count + 1);

        // Resolve supplier if supplierId is provided
        Supplier supplier = null;
        String supplierName = request.getSupplierName();
        if (request.getSupplierId() != null) {
            supplier = supplierRepository.findByStoreIdAndId(storeId, request.getSupplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + request.getSupplierId()));
            if (supplierName == null) {
                supplierName = supplier.getName();
            }
        }

        PurchaseOrder po = PurchaseOrder.builder()
                .store(store)
                .poNumber(poNumber)
                .poDate(request.getPoDate() != null ? request.getPoDate() : LocalDate.now())
                .estimatedArrival(request.getEstimatedArrival())
                .status(PurchaseOrderStatus.DRAFT)
                .supplierName(supplierName)
                .supplier(supplier)
                .supplierCurrency(request.getSupplierCurrency() != null ? request.getSupplierCurrency() : "TRY")
                .exchangeRate(request.getExchangeRate())
                .carrier(request.getCarrier())
                .trackingNumber(request.getTrackingNumber())
                .comment(request.getComment())
                .stockEntryDate(request.getStockEntryDate())
                .build();

        purchaseOrderRepository.save(po);
        log.info("Created purchase order {} for store {}", poNumber, storeId);
        return toDto(po);
    }

    @Transactional
    public PurchaseOrderDto updatePurchaseOrder(UUID storeId, Long poId, UpdatePurchaseOrderRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        if (request.getPoDate() != null) {
            po.setPoDate(request.getPoDate());
        }
        if (request.getEstimatedArrival() != null) {
            po.setEstimatedArrival(request.getEstimatedArrival());
        }
        if (request.getSupplierName() != null) {
            po.setSupplierName(request.getSupplierName());
        }
        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findByStoreIdAndId(storeId, request.getSupplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + request.getSupplierId()));
            po.setSupplier(supplier);
            if (request.getSupplierName() == null) {
                po.setSupplierName(supplier.getName());
            }
        }
        if (request.getSupplierCurrency() != null) {
            po.setSupplierCurrency(request.getSupplierCurrency());
        }
        if (request.getExchangeRate() != null) {
            po.setExchangeRate(request.getExchangeRate());
        }
        if (request.getCarrier() != null) {
            po.setCarrier(request.getCarrier());
        }
        if (request.getTrackingNumber() != null) {
            po.setTrackingNumber(request.getTrackingNumber());
        }
        if (request.getComment() != null) {
            po.setComment(request.getComment());
        }
        if (request.getTransportationCost() != null) {
            po.setTransportationCost(request.getTransportationCost());
        }
        if (request.getStockEntryDate() != null) {
            po.setStockEntryDate(request.getStockEntryDate());
        }

        purchaseOrderRepository.save(po);
        log.info("Updated purchase order {}", poId);
        return toDto(po);
    }

    @Transactional
    public void deletePurchaseOrder(UUID storeId, Long poId) {
        PurchaseOrder po = purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));
        purchaseOrderRepository.delete(po);
        log.info("Deleted purchase order {}", poId);
    }

    // === Status ===

    @Transactional
    public PurchaseOrderDto updateStatus(UUID storeId, Long poId, PurchaseOrderStatus newStatus) {
        PurchaseOrder po = purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        PurchaseOrderStatus oldStatus = po.getStatus();
        po.setStatus(newStatus);
        purchaseOrderRepository.save(po);

        // When PO is closed, update product costs (COGS integration)
        if (newStatus == PurchaseOrderStatus.CLOSED && oldStatus != PurchaseOrderStatus.CLOSED) {
            updateProductCosts(po);
        }

        log.info("Updated purchase order {} status from {} to {}", poId, oldStatus, newStatus);
        return toDto(po);
    }

    // === Items ===

    @Transactional
    public PurchaseOrderDto addItem(UUID storeId, Long poId, AddPurchaseOrderItemRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        TrendyolProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProductId()));

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .product(product)
                .unitsOrdered(request.getUnitsOrdered())
                .unitsPerBox(request.getUnitsPerBox())
                .boxesOrdered(request.getBoxesOrdered())
                .boxDimensions(request.getBoxDimensions())
                .manufacturingCostPerUnit(request.getManufacturingCostPerUnit())
                .transportationCostPerUnit(request.getTransportationCostPerUnit() != null
                        ? request.getTransportationCostPerUnit() : BigDecimal.ZERO)
                .costVatRate(request.getCostVatRate() != null
                        ? request.getCostVatRate() : (product.getVatRate() != null ? product.getVatRate() : 20))
                .hsCode(request.getHsCode())
                .manufacturingCostSupplierCurrency(request.getManufacturingCostSupplierCurrency())
                .labels(request.getLabels())
                .stockEntryDate(request.getStockEntryDate())
                .comment(request.getComment())
                .build();

        purchaseOrderItemRepository.save(item);
        po.getItems().add(item);

        recalculateTotals(po);
        purchaseOrderRepository.save(po);

        log.info("Added item {} to purchase order {}", request.getProductId(), poId);
        return toDto(po);
    }

    @Transactional
    public PurchaseOrderDto updateItem(UUID storeId, Long poId, Long itemId, AddPurchaseOrderItemRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        PurchaseOrderItem item = purchaseOrderItemRepository.findByPurchaseOrderIdAndId(poId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        if (request.getProductId() != null) {
            TrendyolProduct product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProductId()));
            item.setProduct(product);
        }
        if (request.getUnitsOrdered() != null) {
            item.setUnitsOrdered(request.getUnitsOrdered());
        }
        if (request.getUnitsPerBox() != null) {
            item.setUnitsPerBox(request.getUnitsPerBox());
        }
        if (request.getBoxesOrdered() != null) {
            item.setBoxesOrdered(request.getBoxesOrdered());
        }
        if (request.getBoxDimensions() != null) {
            item.setBoxDimensions(request.getBoxDimensions());
        }
        if (request.getManufacturingCostPerUnit() != null) {
            item.setManufacturingCostPerUnit(request.getManufacturingCostPerUnit());
        }
        if (request.getTransportationCostPerUnit() != null) {
            item.setTransportationCostPerUnit(request.getTransportationCostPerUnit());
        }
        if (request.getCostVatRate() != null) {
            item.setCostVatRate(request.getCostVatRate());
        }
        if (request.getHsCode() != null) {
            item.setHsCode(request.getHsCode());
        }
        if (request.getManufacturingCostSupplierCurrency() != null) {
            item.setManufacturingCostSupplierCurrency(request.getManufacturingCostSupplierCurrency());
        }
        if (request.getLabels() != null) {
            item.setLabels(request.getLabels());
        }
        if (request.getStockEntryDate() != null) {
            item.setStockEntryDate(request.getStockEntryDate());
        }
        if (request.getComment() != null) {
            item.setComment(request.getComment());
        }

        purchaseOrderItemRepository.save(item);
        recalculateTotals(po);
        purchaseOrderRepository.save(po);

        log.info("Updated item {} in purchase order {}", itemId, poId);
        return toDto(po);
    }

    @Transactional
    public PurchaseOrderDto removeItem(UUID storeId, Long poId, Long itemId) {
        PurchaseOrder po = purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        PurchaseOrderItem item = purchaseOrderItemRepository.findByPurchaseOrderIdAndId(poId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));

        po.getItems().remove(item);
        purchaseOrderItemRepository.delete(item);

        recalculateTotals(po);
        purchaseOrderRepository.save(po);

        log.info("Removed item {} from purchase order {}", itemId, poId);
        return toDto(po);
    }

    // === Duplicate PO ===

    @Transactional
    public PurchaseOrderDto duplicatePurchaseOrder(UUID storeId, Long poId) {
        PurchaseOrder original = purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        Long count = purchaseOrderRepository.countByStoreId(storeId);
        String poNumber = String.format("PO-%06d", count + 1);

        PurchaseOrder duplicate = PurchaseOrder.builder()
                .store(original.getStore())
                .poNumber(poNumber)
                .poDate(LocalDate.now())
                .estimatedArrival(original.getEstimatedArrival())
                .stockEntryDate(original.getStockEntryDate())
                .status(PurchaseOrderStatus.DRAFT)
                .supplierName(original.getSupplierName())
                .supplier(original.getSupplier())
                .supplierCurrency(original.getSupplierCurrency())
                .exchangeRate(original.getExchangeRate())
                .carrier(original.getCarrier())
                .comment(original.getComment())
                .build();

        purchaseOrderRepository.save(duplicate);

        // Copy items
        for (PurchaseOrderItem originalItem : original.getItems()) {
            PurchaseOrderItem newItem = PurchaseOrderItem.builder()
                    .purchaseOrder(duplicate)
                    .product(originalItem.getProduct())
                    .unitsOrdered(originalItem.getUnitsOrdered())
                    .unitsPerBox(originalItem.getUnitsPerBox())
                    .boxesOrdered(originalItem.getBoxesOrdered())
                    .boxDimensions(originalItem.getBoxDimensions())
                    .manufacturingCostPerUnit(originalItem.getManufacturingCostPerUnit())
                    .transportationCostPerUnit(originalItem.getTransportationCostPerUnit())
                    .costVatRate(originalItem.getCostVatRate())
                    .hsCode(originalItem.getHsCode())
                    .manufacturingCostSupplierCurrency(originalItem.getManufacturingCostSupplierCurrency())
                    .labels(originalItem.getLabels())
                    .stockEntryDate(originalItem.getStockEntryDate())
                    .comment(originalItem.getComment())
                    .build();
            purchaseOrderItemRepository.save(newItem);
            duplicate.getItems().add(newItem);
        }

        recalculateTotals(duplicate);
        purchaseOrderRepository.save(duplicate);

        log.info("Duplicated purchase order {} to {}", poId, duplicate.getId());
        return toDto(duplicate);
    }

    // === Split PO ===

    @Transactional
    public PurchaseOrderDto splitPurchaseOrder(UUID storeId, Long poId, List<Long> itemIds) {
        PurchaseOrder original = purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        if (itemIds == null || itemIds.isEmpty()) {
            throw new IllegalArgumentException("At least one item ID is required to split");
        }

        Long count = purchaseOrderRepository.countByStoreId(storeId);
        String poNumber = String.format("PO-%06d", count + 1);

        PurchaseOrder newPo = PurchaseOrder.builder()
                .store(original.getStore())
                .poNumber(poNumber)
                .poDate(original.getPoDate())
                .estimatedArrival(original.getEstimatedArrival())
                .stockEntryDate(original.getStockEntryDate())
                .status(original.getStatus())
                .supplierName(original.getSupplierName())
                .supplier(original.getSupplier())
                .supplierCurrency(original.getSupplierCurrency())
                .exchangeRate(original.getExchangeRate())
                .carrier(original.getCarrier())
                .parentPo(original)
                .comment("Split from " + original.getPoNumber())
                .build();

        purchaseOrderRepository.save(newPo);

        // Move selected items to the new PO
        List<PurchaseOrderItem> itemsToMove = original.getItems().stream()
                .filter(item -> itemIds.contains(item.getId()))
                .collect(Collectors.toList());

        for (PurchaseOrderItem item : itemsToMove) {
            original.getItems().remove(item);
            item.setPurchaseOrder(newPo);
            purchaseOrderItemRepository.save(item);
            newPo.getItems().add(item);
        }

        recalculateTotals(original);
        purchaseOrderRepository.save(original);
        recalculateTotals(newPo);
        purchaseOrderRepository.save(newPo);

        log.info("Split {} items from PO {} to new PO {}", itemIds.size(), poId, newPo.getId());
        return toDto(newPo);
    }

    // === Attachments ===

    @Transactional
    public AttachmentDto addAttachment(UUID storeId, Long poId, String fileName, String fileType, long fileSize, byte[] fileData) {
        PurchaseOrder po = purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        PurchaseOrderAttachment attachment = PurchaseOrderAttachment.builder()
                .purchaseOrder(po)
                .fileName(fileName)
                .fileType(fileType)
                .fileSize(fileSize)
                .fileData(fileData)
                .build();

        attachmentRepository.save(attachment);
        log.info("Added attachment '{}' to PO {}", fileName, poId);
        return toAttachmentDto(attachment);
    }

    public List<AttachmentDto> getAttachments(UUID storeId, Long poId) {
        // Verify PO belongs to store
        purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        return attachmentRepository.findByPurchaseOrderIdOrderByUploadedAtDesc(poId).stream()
                .map(this::toAttachmentDto)
                .collect(Collectors.toList());
    }

    public PurchaseOrderAttachment getAttachmentWithData(UUID storeId, Long poId, Long attachmentId) {
        purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        return attachmentRepository.findByPurchaseOrderIdAndId(poId, attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
    }

    @Transactional
    public void deleteAttachment(UUID storeId, Long poId, Long attachmentId) {
        purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        PurchaseOrderAttachment attachment = attachmentRepository.findByPurchaseOrderIdAndId(poId, attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));

        attachmentRepository.delete(attachment);
        log.info("Deleted attachment {} from PO {}", attachmentId, poId);
    }

    // === Advanced Search/Filter ===

    public List<PurchaseOrderSummaryDto> searchPurchaseOrders(UUID storeId, String search,
                                                              PurchaseOrderStatus status, Long supplierId,
                                                              LocalDate startDate, LocalDate endDate) {
        List<PurchaseOrder> orders;

        if (search != null && !search.isEmpty()) {
            orders = purchaseOrderRepository.searchByTerm(storeId, search);
            // Apply additional filters in-memory for combined search
            if (status != null) {
                orders = orders.stream().filter(po -> po.getStatus() == status).collect(Collectors.toList());
            }
            if (supplierId != null) {
                orders = orders.stream()
                        .filter(po -> po.getSupplier() != null && po.getSupplier().getId().equals(supplierId))
                        .collect(Collectors.toList());
            }
            // Apply date filter in-memory
            if (startDate != null && endDate != null) {
                orders = orders.stream()
                        .filter(po -> !po.getPoDate().isBefore(startDate) && !po.getPoDate().isAfter(endDate))
                        .collect(Collectors.toList());
            }
        } else if (supplierId != null && status != null) {
            orders = purchaseOrderRepository.findByStoreIdAndSupplierIdAndStatus(storeId, supplierId, status);
            // Apply date filter in-memory for combined filters
            if (startDate != null && endDate != null) {
                orders = orders.stream()
                        .filter(po -> !po.getPoDate().isBefore(startDate) && !po.getPoDate().isAfter(endDate))
                        .collect(Collectors.toList());
            }
        } else if (supplierId != null) {
            orders = purchaseOrderRepository.findByStoreIdAndSupplierIdOrderByPoDateDesc(storeId, supplierId);
            if (startDate != null && endDate != null) {
                orders = orders.stream()
                        .filter(po -> !po.getPoDate().isBefore(startDate) && !po.getPoDate().isAfter(endDate))
                        .collect(Collectors.toList());
            }
        } else if (startDate != null && endDate != null) {
            if (status != null) {
                orders = purchaseOrderRepository.findByStoreIdAndPoDateBetweenAndStatus(
                        storeId, startDate, endDate, status);
            } else {
                orders = purchaseOrderRepository.findByStoreIdAndPoDateBetween(storeId, startDate, endDate);
            }
        } else if (status != null) {
            orders = purchaseOrderRepository.findByStoreIdAndStatusOrderByPoDateDesc(storeId, status);
        } else {
            orders = purchaseOrderRepository.findByStoreIdOrderByPoDateDesc(storeId);
        }

        return orders.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    private AttachmentDto toAttachmentDto(PurchaseOrderAttachment attachment) {
        return AttachmentDto.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

    // === Internal Methods ===

    private void recalculateTotals(PurchaseOrder po) {
        int totalUnits = 0;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (PurchaseOrderItem item : po.getItems()) {
            totalUnits += item.getUnitsOrdered();
            totalCost = totalCost.add(item.getTotalCost());
        }

        po.setTotalUnits(totalUnits);
        po.setTotalCost(totalCost);
    }

    private void updateProductCosts(PurchaseOrder po) {
        LocalDate earliestStockDate = null;

        for (PurchaseOrderItem item : po.getItems()) {
            TrendyolProduct product = item.getProduct();

            // Effective stock entry date: item-level override → PO-level → poDate
            LocalDate effectiveStockDate = getEffectiveStockEntryDate(po, item);

            // Track earliest date for FIFO redistribution
            if (earliestStockDate == null || effectiveStockDate.isBefore(earliestStockDate)) {
                earliestStockDate = effectiveStockDate;
            }

            // Calculate total cost per unit
            BigDecimal totalCostPerUnit = item.getManufacturingCostPerUnit()
                    .add(item.getTransportationCostPerUnit() != null
                            ? item.getTransportationCostPerUnit() : BigDecimal.ZERO);

            // Get VAT rate from product or PO item, default to 20%
            Integer vatRate = item.getCostVatRate();
            if (vatRate == null) {
                vatRate = product.getVatRate() != null ? product.getVatRate() : 20;
            }

            // Create new cost and stock entry with effective stock date
            CostAndStockInfo newEntry = CostAndStockInfo.builder()
                    .quantity(item.getUnitsOrdered())
                    .unitCost(totalCostPerUnit.doubleValue())
                    .costVatRate(vatRate)
                    .stockDate(effectiveStockDate)
                    .usedQuantity(0)
                    .costSource("PURCHASE_ORDER")
                    .build();

            // Get existing cost history or create new list
            List<CostAndStockInfo> costHistory = product.getCostAndStockInfo();
            if (costHistory == null) {
                costHistory = new ArrayList<>();
            }

            // Add new entry to history
            costHistory.add(newEntry);
            product.setCostAndStockInfo(costHistory);

            productRepository.save(product);
            log.info("Updated COGS for product {} from PO {}: {} units at {} per unit (VAT {}%), stockDate={}",
                    product.getId(), po.getPoNumber(), item.getUnitsOrdered(), totalCostPerUnit, vatRate, effectiveStockDate);
        }

        // FIFO redistribution: Recalculate stock allocation from the earliest effective date
        if (earliestStockDate != null) {
            UUID storeId = po.getStore().getId();
            log.info("Triggering FIFO redistribution for store {} from date {}", storeId, earliestStockDate);
            stockSyncService.redistributeStockFIFO(storeId, earliestStockDate);
        }
    }

    /**
     * Get effective stock entry date for a PO item.
     * Priority: item-level stockEntryDate → PO-level stockEntryDate → poDate
     */
    private LocalDate getEffectiveStockEntryDate(PurchaseOrder po, PurchaseOrderItem item) {
        if (item.getStockEntryDate() != null) {
            return item.getStockEntryDate();
        }
        if (po.getStockEntryDate() != null) {
            return po.getStockEntryDate();
        }
        return po.getPoDate();
    }

    // === Mapping Methods ===

    private PurchaseOrderDto toDto(PurchaseOrder po) {
        return PurchaseOrderDto.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .poDate(po.getPoDate())
                .estimatedArrival(po.getEstimatedArrival())
                .stockEntryDate(po.getStockEntryDate())
                .status(po.getStatus())
                .supplierName(po.getSupplierName())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierCurrency(po.getSupplierCurrency())
                .exchangeRate(po.getExchangeRate())
                .parentPoId(po.getParentPo() != null ? po.getParentPo().getId() : null)
                .carrier(po.getCarrier())
                .trackingNumber(po.getTrackingNumber())
                .comment(po.getComment())
                .transportationCost(po.getTransportationCost())
                .totalCost(po.getTotalCost())
                .totalUnits(po.getTotalUnits())
                .items(po.getItems().stream().map(this::toItemDto).collect(Collectors.toList()))
                .itemCount(po.getItems().size())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private PurchaseOrderSummaryDto toSummaryDto(PurchaseOrder po) {
        return PurchaseOrderSummaryDto.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .poDate(po.getPoDate())
                .estimatedArrival(po.getEstimatedArrival())
                .stockEntryDate(po.getStockEntryDate())
                .status(po.getStatus())
                .supplierName(po.getSupplierName())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierCurrency(po.getSupplierCurrency())
                .parentPoId(po.getParentPo() != null ? po.getParentPo().getId() : null)
                .totalCost(po.getTotalCost())
                .totalUnits(po.getTotalUnits())
                .itemCount(po.getItems().size())
                .build();
    }

    private PurchaseOrderItemDto toItemDto(PurchaseOrderItem item) {
        TrendyolProduct product = item.getProduct();
        BigDecimal totalCostPerUnit = item.getManufacturingCostPerUnit()
                .add(item.getTransportationCostPerUnit() != null
                        ? item.getTransportationCostPerUnit() : BigDecimal.ZERO);

        return PurchaseOrderItemDto.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getTitle())
                .productBarcode(product.getBarcode())
                .productImage(product.getImage())
                .unitsOrdered(item.getUnitsOrdered())
                .unitsPerBox(item.getUnitsPerBox())
                .boxesOrdered(item.getBoxesOrdered())
                .boxDimensions(item.getBoxDimensions())
                .manufacturingCostPerUnit(item.getManufacturingCostPerUnit())
                .transportationCostPerUnit(item.getTransportationCostPerUnit())
                .costVatRate(item.getCostVatRate())
                .totalCostPerUnit(totalCostPerUnit)
                .totalCost(item.getTotalCost())
                .hsCode(item.getHsCode())
                .manufacturingCostSupplierCurrency(item.getManufacturingCostSupplierCurrency())
                .labels(item.getLabels())
                .stockEntryDate(item.getStockEntryDate())
                .comment(item.getComment())
                .build();
    }
}
