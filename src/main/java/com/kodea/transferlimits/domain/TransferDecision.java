package com.kodea.transferlimits.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferDecision(
    boolean approved,
    String accountId,
    TransferChannel channel,
    BigDecimal amount,
    BigDecimal consumedToday,
    BigDecimal remainingToday,
    Instant decidedAt,
    String rejectionReason) {}
