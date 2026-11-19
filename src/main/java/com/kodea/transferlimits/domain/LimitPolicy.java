package com.kodea.transferlimits.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LimitPolicy(
    String accountId,
    TransferChannel channel,
    BigDecimal dailyLimit,
    BigDecimal perOperationLimit,
    BigDecimal consumedToday,
    LocalDate usageDate) {}
