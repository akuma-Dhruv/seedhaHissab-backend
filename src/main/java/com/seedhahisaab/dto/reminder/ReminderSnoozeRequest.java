package com.seedhahisaab.dto.reminder;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** Body for {@code PATCH /reminders/{id}/snooze}. */
@Data
public class ReminderSnoozeRequest {

    @NotNull(message = "New due date is required")
    private LocalDate newDueDate;
}
