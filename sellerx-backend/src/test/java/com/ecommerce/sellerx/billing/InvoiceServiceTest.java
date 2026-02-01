package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.billing.config.SubscriptionConfig;
import com.ecommerce.sellerx.billing.service.InvoiceService;
import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.common.TestDataBuilder;
import com.ecommerce.sellerx.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("InvoiceService")
class InvoiceServiceTest extends BaseUnitTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    private SubscriptionConfig config;
    private InvoiceService invoiceService;

    private User testUser;
    private SubscriptionPlan starterPlan;
    private SubscriptionPrice monthlyPrice;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        config = new SubscriptionConfig();
        config.setGracePeriodDays(3);
        SubscriptionConfig.Invoice invoiceConfig = new SubscriptionConfig.Invoice();
        invoiceConfig.setTaxRate(20);
        invoiceConfig.setSeries("SEL");
        config.setInvoice(invoiceConfig);

        invoiceService = new InvoiceService(invoiceRepository, config);

        testUser = TestDataBuilder.user().build();
        testUser.setId(1L);

        starterPlan = SubscriptionPlan.builder()
                .code("STARTER")
                .name("Starter Plan")
                .sortOrder(1)
                .isActive(true)
                .build();
        starterPlan.setId(UUID.randomUUID());

        monthlyPrice = SubscriptionPrice.builder()
                .plan(starterPlan)
                .billingCycle(BillingCycle.MONTHLY)
                .priceAmount(new BigDecimal("299.00"))
                .currency("TRY")
                .isActive(true)
                .build();
        monthlyPrice.setId(UUID.randomUUID());

        subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .plan(starterPlan)
                .price(monthlyPrice)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .cancelAtPeriodEnd(false)
                .autoRenew(true)
                .build();
    }

    // =========================================================================
    // createInvoice
    // =========================================================================

    @Nested
    @DisplayName("createInvoice")
    class CreateInvoice {

        @Test
        @DisplayName("should create invoice with correct tax calculation")
        void shouldCreateInvoiceWithCorrectTax() {
            LocalDateTime periodStart = LocalDateTime.now();
            LocalDateTime periodEnd = periodStart.plusMonths(1);

            when(invoiceRepository.findMaxInvoiceNumberByPrefix(anyString())).thenReturn(null);
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
                Invoice i = inv.getArgument(0);
                i.setId(UUID.randomUUID());
                return i;
            });

            Invoice result = invoiceService.createInvoice(subscription, periodStart, periodEnd);

            assertThat(result).isNotNull();
            assertThat(result.getSubscription()).isEqualTo(subscription);
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PENDING);
            assertThat(result.getCurrency()).isEqualTo("TRY");
            assertThat(result.getInvoiceSeries()).isEqualTo("SEL");

            // Tax calculation: 299.00 * 20 / 100 = 59.80
            BigDecimal expectedTax = new BigDecimal("299.00")
                    .multiply(new BigDecimal("20"))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal expectedTotal = new BigDecimal("299.00").add(expectedTax);

            assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("299.00"));
            assertThat(result.getTaxAmount()).isEqualByComparingTo(expectedTax);
            assertThat(result.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        }

        @Test
        @DisplayName("should generate sequential invoice numbers")
        void shouldGenerateSequentialInvoiceNumbers() {
            LocalDateTime periodStart = LocalDateTime.now();
            LocalDateTime periodEnd = periodStart.plusMonths(1);

            when(invoiceRepository.findMaxInvoiceNumberByPrefix(anyString())).thenReturn(5);
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
                Invoice i = inv.getArgument(0);
                i.setId(UUID.randomUUID());
                return i;
            });

            Invoice result = invoiceService.createInvoice(subscription, periodStart, periodEnd);

            assertThat(result.getInvoiceNumber()).matches("INV-\\d{4}-000006");
        }

        @Test
        @DisplayName("should start invoice numbering at 1 when no existing invoices")
        void shouldStartNumberingAtOne() {
            LocalDateTime periodStart = LocalDateTime.now();
            LocalDateTime periodEnd = periodStart.plusMonths(1);

            when(invoiceRepository.findMaxInvoiceNumberByPrefix(anyString())).thenReturn(null);
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
                Invoice i = inv.getArgument(0);
                i.setId(UUID.randomUUID());
                return i;
            });

            Invoice result = invoiceService.createInvoice(subscription, periodStart, periodEnd);

            assertThat(result.getInvoiceNumber()).matches("INV-\\d{4}-000001");
        }

        @Test
        @DisplayName("should set due date 7 days after period start")
        void shouldSetDueDate() {
            LocalDateTime periodStart = LocalDateTime.of(2026, 2, 1, 0, 0);
            LocalDateTime periodEnd = periodStart.plusMonths(1);

            when(invoiceRepository.findMaxInvoiceNumberByPrefix(anyString())).thenReturn(null);
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
                Invoice i = inv.getArgument(0);
                i.setId(UUID.randomUUID());
                return i;
            });

            Invoice result = invoiceService.createInvoice(subscription, periodStart, periodEnd);

            assertThat(result.getDueDate()).isEqualTo(periodStart.plusDays(7));
            assertThat(result.getBillingPeriodStart()).isEqualTo(periodStart);
            assertThat(result.getBillingPeriodEnd()).isEqualTo(periodEnd);
        }

        @Test
        @DisplayName("should include line items")
        void shouldIncludeLineItems() {
            LocalDateTime periodStart = LocalDateTime.now();
            LocalDateTime periodEnd = periodStart.plusMonths(1);

            when(invoiceRepository.findMaxInvoiceNumberByPrefix(anyString())).thenReturn(null);
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
                Invoice i = inv.getArgument(0);
                i.setId(UUID.randomUUID());
                return i;
            });

            Invoice result = invoiceService.createInvoice(subscription, periodStart, periodEnd);

            assertThat(result.getLineItems()).hasSize(1);
            assertThat(result.getLineItems().get(0).get("description").toString())
                    .contains("Starter Plan");
        }
    }

    // =========================================================================
    // markAsPaid
    // =========================================================================

    @Nested
    @DisplayName("markAsPaid")
    class MarkAsPaid {

        @Test
        @DisplayName("should mark invoice as paid with timestamp")
        void shouldMarkAsPaid() {
            UUID invoiceId = UUID.randomUUID();
            Invoice invoice = buildInvoice(invoiceId, InvoiceStatus.PENDING);

            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

            Invoice result = invoiceService.markAsPaid(invoiceId);

            assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAID);
            assertThat(result.getPaidAt()).isNotNull();
        }
    }

    // =========================================================================
    // markAsFailed
    // =========================================================================

    @Nested
    @DisplayName("markAsFailed")
    class MarkAsFailed {

        @Test
        @DisplayName("should mark invoice as failed")
        void shouldMarkAsFailed() {
            UUID invoiceId = UUID.randomUUID();
            Invoice invoice = buildInvoice(invoiceId, InvoiceStatus.PENDING);

            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

            Invoice result = invoiceService.markAsFailed(invoiceId);

            assertThat(result.getStatus()).isEqualTo(InvoiceStatus.FAILED);
        }
    }

    // =========================================================================
    // voidInvoice
    // =========================================================================

    @Nested
    @DisplayName("voidInvoice")
    class VoidInvoice {

        @Test
        @DisplayName("should void pending invoice")
        void shouldVoidPendingInvoice() {
            UUID invoiceId = UUID.randomUUID();
            Invoice invoice = buildInvoice(invoiceId, InvoiceStatus.PENDING);

            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

            Invoice result = invoiceService.voidInvoice(invoiceId);

            assertThat(result.getStatus()).isEqualTo(InvoiceStatus.VOID);
        }

        @Test
        @DisplayName("should throw when voiding paid invoice")
        void shouldThrowWhenVoidingPaidInvoice() {
            UUID invoiceId = UUID.randomUUID();
            Invoice invoice = buildInvoice(invoiceId, InvoiceStatus.PAID);

            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

            assertThatThrownBy(() -> invoiceService.voidInvoice(invoiceId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot void a paid invoice");
        }
    }

    // =========================================================================
    // findById
    // =========================================================================

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return invoice when found")
        void shouldReturnInvoiceWhenFound() {
            UUID invoiceId = UUID.randomUUID();
            Invoice invoice = buildInvoice(invoiceId, InvoiceStatus.PENDING);

            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

            Invoice result = invoiceService.findById(invoiceId);

            assertThat(result).isEqualTo(invoice);
        }

        @Test
        @DisplayName("should throw when invoice not found")
        void shouldThrowWhenNotFound() {
            UUID invoiceId = UUID.randomUUID();
            when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invoiceService.findById(invoiceId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invoice not found");
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private Invoice buildInvoice(UUID id, InvoiceStatus status) {
        return Invoice.builder()
                .id(id)
                .subscription(subscription)
                .user(testUser)
                .invoiceNumber("INV-2026-000001")
                .invoiceSeries("SEL")
                .status(status)
                .subtotal(new BigDecimal("299.00"))
                .taxRate(new BigDecimal("20"))
                .taxAmount(new BigDecimal("59.80"))
                .totalAmount(new BigDecimal("358.80"))
                .currency("TRY")
                .billingPeriodStart(LocalDateTime.now())
                .billingPeriodEnd(LocalDateTime.now().plusMonths(1))
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();
    }
}
