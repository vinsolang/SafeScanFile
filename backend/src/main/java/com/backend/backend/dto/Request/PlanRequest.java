package com.backend.backend.dto.Request;

import java.math.BigDecimal;

public record PlanRequest(
    String name,
    BigDecimal price,
    Integer scanLimit,
    Integer durationDays,
    Boolean active
) {}
