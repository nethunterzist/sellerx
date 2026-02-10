package com.ecommerce.sellerx.expenses;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled service that generates recurring expense instances.
 * Runs daily at 00:05 Turkey time to create expense records
 * based on recurring templates (daily, weekly, monthly, yearly).
 */
@Service
@Slf4j
@AllArgsConstructor
public class ExpenseRecurrenceScheduledService {

    private final StoreExpenseRepository expenseRepository;

    /**
     * Generate recurring expense instances for today.
     * Runs daily at 00:05 Turkey time.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Europe/Istanbul")
    @Transactional
    public void generateRecurringExpenses() {
        LocalDate today = LocalDate.now();
        log.info("[RecurringExpense] Starting generation for date: {}", today);

        try {
            List<StoreExpense> templates = expenseRepository.findActiveRecurringTemplates(today);
            log.info("[RecurringExpense] Found {} active templates", templates.size());

            int generated = 0;
            for (StoreExpense template : templates) {
                if (shouldGenerateInstance(template, today)) {
                    createInstance(template, today);
                    template.setLastGeneratedDate(today);
                    expenseRepository.save(template);
                    generated++;
                    log.debug("[RecurringExpense] Generated instance for template: {} ({})",
                            template.getName(), template.getId());
                }
            }

            log.info("[RecurringExpense] Completed. Generated {} expenses for {}", generated, today);
        } catch (Exception e) {
            log.error("[RecurringExpense] Error generating recurring expenses", e);
        }
    }

    /**
     * Manual trigger for testing or catch-up purposes.
     * @param date The date to generate expenses for
     * @return Number of expenses generated
     */
    @Transactional
    public int generateRecurringExpensesForDate(LocalDate date) {
        log.info("[RecurringExpense] Manual generation for date: {}", date);

        List<StoreExpense> templates = expenseRepository.findActiveRecurringTemplates(date);
        int generated = 0;

        for (StoreExpense template : templates) {
            if (shouldGenerateInstance(template, date)) {
                createInstance(template, date);
                template.setLastGeneratedDate(date);
                expenseRepository.save(template);
                generated++;
            }
        }

        log.info("[RecurringExpense] Manual generation completed. Generated {} expenses for {}", generated, date);
        return generated;
    }

    /**
     * Determines if an expense instance should be generated for the given template and date.
     */
    private boolean shouldGenerateInstance(StoreExpense template, LocalDate today) {
        LocalDate startDate = template.getDate().toLocalDate();

        // Don't generate before start date
        if (today.isBefore(startDate)) {
            return false;
        }

        // Don't generate after end date
        if (template.getEndDate() != null && today.isAfter(template.getEndDate().toLocalDate())) {
            return false;
        }

        // Check if already generated for today
        if (template.getLastGeneratedDate() != null &&
                !template.getLastGeneratedDate().isBefore(today)) {
            return false;
        }

        // Also check if instance exists in database (safety check)
        if (expenseRepository.existsInstanceForDate(template.getId(), today)) {
            return false;
        }

        // Check frequency-specific rules
        return switch (template.getFrequency()) {
            case DAILY -> true;
            case WEEKLY -> startDate.getDayOfWeek() == today.getDayOfWeek();
            case MONTHLY -> {
                int templateDay = startDate.getDayOfMonth();
                int todayDay = today.getDayOfMonth();
                int lastDayOfMonth = today.lengthOfMonth();

                // If template day is 31 and current month has fewer days,
                // use the last day of the month
                if (templateDay > lastDayOfMonth) {
                    yield todayDay == lastDayOfMonth;
                }
                yield templateDay == todayDay;
            }
            case YEARLY -> startDate.getMonth() == today.getMonth() &&
                    startDate.getDayOfMonth() == today.getDayOfMonth();
            case ONE_TIME -> false; // One-time expenses don't recur
        };
    }

    /**
     * Creates an expense instance from a template.
     */
    private void createInstance(StoreExpense template, LocalDate date) {
        StoreExpense instance = new StoreExpense();
        instance.setStore(template.getStore());
        instance.setName(template.getName());
        instance.setAmount(template.getAmount());
        instance.setExpenseCategory(template.getExpenseCategory());
        instance.setProduct(template.getProduct());
        instance.setFrequency(template.getFrequency()); // Keep original frequency
        instance.setDate(date.atStartOfDay());
        instance.setVatRate(template.getVatRate());
        instance.setIsVatDeductible(template.getIsVatDeductible());
        instance.setIsRecurringTemplate(false);
        instance.setParentExpense(template);
        instance.calculateVatFields();

        expenseRepository.save(instance);
    }
}
