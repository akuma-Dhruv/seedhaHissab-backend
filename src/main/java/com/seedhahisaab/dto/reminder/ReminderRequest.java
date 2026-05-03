package com.seedhahisaab.dto.reminder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Body for {@code POST /reminders} and {@code PUT /reminders/{id}}.
 *
 * <p>All link fields are optional — a reminder can be a free-floating
 * note, or attached to up to one of each context type. The server never
 * trusts {@code createdByUserId} from the request; it always derives it
 * from the JWT.
 */
@Data
public class ReminderRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be 255 characters or fewer")
    private String title;

    @Size(max = 1024, message = "Description must be 1024 characters or fewer")
    private String description;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    /** Stored as the transaction's stable {@code root_transaction_id}. */
    private UUID linkedTransactionId;

    private UUID linkedProjectId;

    @Size(max = 255, message = "Counterparty name must be 255 characters or fewer")
    private String linkedCounterpartyName;
}
