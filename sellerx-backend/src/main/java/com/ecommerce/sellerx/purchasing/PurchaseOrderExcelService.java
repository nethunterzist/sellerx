package com.ecommerce.sellerx.purchasing;

import com.ecommerce.sellerx.products.TrendyolProduct;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderExcelService {

    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final TrendyolProductRepository productRepository;

    private static final String[] EXPORT_HEADERS = {
            "Product Name", "Barcode", "Units Ordered", "Units Per Box",
            "Boxes Ordered", "Manufacturing Cost", "Transportation Cost",
            "HS Code", "Labels", "Comment"
    };

    public byte[] exportToExcel(PurchaseOrderDto po) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PO Items");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (PurchaseOrderItemDto item : po.getItems()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.getProductName() != null ? item.getProductName() : "");
                row.createCell(1).setCellValue(item.getProductBarcode() != null ? item.getProductBarcode() : "");
                row.createCell(2).setCellValue(item.getUnitsOrdered() != null ? item.getUnitsOrdered() : 0);
                row.createCell(3).setCellValue(item.getUnitsPerBox() != null ? item.getUnitsPerBox() : 0);
                row.createCell(4).setCellValue(item.getBoxesOrdered() != null ? item.getBoxesOrdered() : 0);
                row.createCell(5).setCellValue(item.getManufacturingCostPerUnit() != null ? item.getManufacturingCostPerUnit().doubleValue() : 0);
                row.createCell(6).setCellValue(item.getTransportationCostPerUnit() != null ? item.getTransportationCostPerUnit().doubleValue() : 0);
                row.createCell(7).setCellValue(item.getHsCode() != null ? item.getHsCode() : "");
                row.createCell(8).setCellValue(item.getLabels() != null ? item.getLabels() : "");
                row.createCell(9).setCellValue(item.getComment() != null ? item.getComment() : "");
            }

            // Auto-size columns
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export PO to Excel", e);
        }
    }

    @Transactional
    public PurchaseOrderDto importFromExcel(UUID storeId, Long poId, InputStream inputStream) {
        PurchaseOrder po = purchaseOrderRepository.findByStoreIdAndId(storeId, poId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + poId));

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            List<AddPurchaseOrderItemRequest> items = new ArrayList<>();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String barcode = getStringCellValue(row, 1);
                if (barcode == null || barcode.isEmpty()) continue;

                // Find product by barcode
                TrendyolProduct product = productRepository.findByStoreIdAndBarcode(storeId, barcode).orElse(null);
                if (product == null) {
                    log.warn("Product not found for barcode {} in store {}, skipping row {}", barcode, storeId, i);
                    continue;
                }

                AddPurchaseOrderItemRequest itemReq = AddPurchaseOrderItemRequest.builder()
                        .productId(product.getId())
                        .unitsOrdered(getIntCellValue(row, 2))
                        .unitsPerBox(getIntCellValue(row, 3))
                        .boxesOrdered(getIntCellValue(row, 4))
                        .manufacturingCostPerUnit(getDecimalCellValue(row, 5))
                        .transportationCostPerUnit(getDecimalCellValue(row, 6))
                        .hsCode(getStringCellValue(row, 7))
                        .labels(getStringCellValue(row, 8))
                        .comment(getStringCellValue(row, 9))
                        .build();

                items.add(itemReq);
            }

            // Add items to PO
            PurchaseOrderDto result = null;
            for (AddPurchaseOrderItemRequest item : items) {
                result = purchaseOrderService.addItem(storeId, poId, item);
            }

            log.info("Imported {} items from Excel to PO {}", items.size(), poId);
            return result != null ? result : purchaseOrderService.getPurchaseOrder(storeId, poId);
        } catch (IOException e) {
            throw new RuntimeException("Failed to import Excel file", e);
        }
    }

    private String getStringCellValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return null;
    }

    private Integer getIntCellValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        if (cell.getCellType() == CellType.STRING) {
            try { return Integer.parseInt(cell.getStringCellValue()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private BigDecimal getDecimalCellValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
        if (cell.getCellType() == CellType.STRING) {
            try { return new BigDecimal(cell.getStringCellValue()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
