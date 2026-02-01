package com.ecommerce.sellerx.stores;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents the status of a single sync phase.
 * Used for tracking parallel sync execution progress.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhaseStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Status of this phase: PENDING, ACTIVE, COMPLETED, FAILED
     */
    private PhaseStatusType status;

    /**
     * When this phase started
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;

    /**
     * When this phase completed (success or failure)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    /**
     * Error message if status is FAILED
     */
    private String errorMessage;

    /**
     * Progress percentage (0-100) for long-running phases
     * Optional - only used for phases like HISTORICAL that process many chunks
     */
    private Integer progress;

    // Convenience factory methods
    public static PhaseStatus pending() {
        return PhaseStatus.builder()
                .status(PhaseStatusType.PENDING)
                .build();
    }

    public static PhaseStatus active() {
        return PhaseStatus.builder()
                .status(PhaseStatusType.ACTIVE)
                .startedAt(LocalDateTime.now())
                .build();
    }

    public static PhaseStatus completed() {
        return PhaseStatus.builder()
                .status(PhaseStatusType.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
    }

    public static PhaseStatus failed(String errorMessage) {
        return PhaseStatus.builder()
                .status(PhaseStatusType.FAILED)
                .completedAt(LocalDateTime.now())
                .errorMessage(errorMessage)
                .build();
    }

    public static PhaseStatus activeWithProgress(int progress) {
        return PhaseStatus.builder()
                .status(PhaseStatusType.ACTIVE)
                .startedAt(LocalDateTime.now())
                .progress(progress)
                .build();
    }
}
