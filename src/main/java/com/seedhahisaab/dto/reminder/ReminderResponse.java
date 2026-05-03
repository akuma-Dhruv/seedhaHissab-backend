package com.seedhahisaab.dto.reminder;

import com.seedhahisaab.domain.ReminderStatus;
import com.seedhahisaab.entity.Reminder;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Wire shape returned by every {@code /reminders} endpoint. */
@Data
@Builder
public class ReminderResponse {

    private UUID id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private ReminderStatus status;
    private UUID linkedTransactionId;
    private UUID linkedProjectId;
    private String linkedCounterpartyName;
    private UUID createdByUserId;
    private Instant createdAt;
    private Instant updatedAt;

    public static ReminderResponse from(Reminder r) {
        return ReminderResponse.builder()
                .id(r.getId())
                .title(r.getTitle())
                .description(r.getDescription())
                .dueDate(r.getDueDate())
                .status(r.getStatus())
                .linkedTransactionId(r.getLinkedTransactionId())
                .linkedProjectId(r.getLinkedProjectId())
                .linkedCounterpartyName(r.getLinkedCounterpartyName())
                .createdByUserId(r.getCreatedByUserId())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
