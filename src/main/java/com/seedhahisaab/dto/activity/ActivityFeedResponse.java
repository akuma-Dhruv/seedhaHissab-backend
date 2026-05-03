package com.seedhahisaab.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated activity feed envelope.
 *
 * <p>The backend returns a flat ordered stream — date grouping is a frontend
 * concern. {@link #hasMore} is derived from whether the merged-and-trimmed
 * window still had a next item; {@link #total} is left out intentionally to
 * avoid forcing an expensive count query across N sources.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityFeedResponse {
    private List<ActivityItemDTO> items;
    private int page;
    private int limit;
    private boolean hasMore;
}
