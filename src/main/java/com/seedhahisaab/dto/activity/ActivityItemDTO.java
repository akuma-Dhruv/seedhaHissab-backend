package com.seedhahisaab.dto.activity;

import com.seedhahisaab.domain.ActivityType;
import com.seedhahisaab.domain.FinancialVisibilityScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Generic, presentation-oriented activity item.
 *
 * <p>This DTO is intentionally <strong>not specialised</strong> for any one
 * source. Adding a new activity type (e.g. {@code VENDOR_INVOICE_RECEIVED})
 * must not require new top-level fields — anything source-specific belongs in
 * {@link #extraData}. That keeps the FE rendering layer, the export pipeline,
 * and any future notification subscriber working off a single shape.
 *
 * <p><strong>Server narrates, frontend renders.</strong> {@link #title} and
 * {@link #subtitle} are produced server-side from the underlying row plus
 * the caller's identity (so we can say "You" vs "Dhruv"). The frontend never
 * builds these strings — that would drift across web/mobile/exports.
 *
 * <p>{@link #activityKey} is a stable, deterministic identifier of the form
 * {@code "<TYPE>:<ENTITY_ID>:<EPOCH_MS>"}. It exists for FE dedupe, future
 * cursor pagination, and any later notification fan-out — never for storage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityItemDTO {

    /**
     * Stable identifier built as {@code type:entityId:epochMs}. Two activity
     * items with the same {@code activityKey} represent the same logical
     * event and may be deduped on the client.
     */
    private String activityKey;

    private ActivityType type;
    private Instant timestamp;

    private UUID actorUserId;
    private String actorName;

    /** Server-narrated headline, e.g. "You recorded ₹50,000 income". */
    private String title;
    /** Server-narrated supporting line, e.g. "Project Alpha · Counterparty Ravi". */
    private String subtitle;

    /** Optional monetary value tied to the event. Null for non-financial events. */
    private BigDecimal amount;

    private FinancialVisibilityScope visibilityScope;

    /** Stable string tag of the linked entity, e.g. {@code "TRANSACTION"}, {@code "INSTALLMENT"}. */
    private String linkedEntityType;
    private UUID linkedEntityId;

    /** Optional small badge string (e.g. {@code "OMITTED"}). FE may render as a chip. */
    private String badge;
    /** Optional status string (e.g. {@code "COMPLETED"}, {@code "OVERDUE"}). */
    private String status;

    /**
     * Free-form metadata. Keep it small. Anything that influences how the
     * row renders should live here, not as a new top-level field.
     */
    private Map<String, Object> extraData;
}
