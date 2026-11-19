package com.kodea.transferlimits.application;

import com.kodea.transferlimits.domain.LimitPolicy;
import com.kodea.transferlimits.domain.LimitViolation;
import com.kodea.transferlimits.domain.TransferChannel;
import com.kodea.transferlimits.domain.TransferDecision;
import com.kodea.transferlimits.infrastructure.AccountChannelLimitEntity;
import com.kodea.transferlimits.infrastructure.AccountChannelLimitKey;
import com.kodea.transferlimits.infrastructure.AccountChannelLimitRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LimitService {
  private static final String OPERATION_REJECTED =
      "Amount ${amount} exceeds per-operation limit ${limit}.";
  private static final String DAILY_REJECTED =
      "Amount ${amount} exceeds remaining daily limit ${remaining}.";
  private final AccountChannelLimitRepository repository;
  private final Clock clock;

  public LimitService(AccountChannelLimitRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<LimitPolicy> getLimits(String accountId) {
    LocalDate today = LocalDate.now(clock);
    return repository.findByAccountIdOrderByChannel(accountId).stream()
        .map(entity -> toPolicy(entity, today))
        .toList();
  }

  @Transactional
  public LimitPolicy updateLimit(
      String accountId, TransferChannel channel, BigDecimal dailyLimit, BigDecimal operationLimit) {
    if (operationLimit.compareTo(dailyLimit) > 0) {
      throw new LimitViolation(
          "OPERATION_LIMIT_ABOVE_DAILY_LIMIT", "Per-operation limit cannot exceed daily limit.");
    }
    LocalDate today = LocalDate.now(clock);
    AccountChannelLimitKey key = new AccountChannelLimitKey(accountId, channel);
    AccountChannelLimitEntity entity =
        repository
            .findById(key)
            .orElseGet(() ->
                new AccountChannelLimitEntity(
                    accountId, channel, dailyLimit, operationLimit, BigDecimal.ZERO, today));
    BigDecimal consumed = currentUsage(entity, today);
    if (dailyLimit.compareTo(consumed) < 0) {
      throw new LimitViolation(
          "DAILY_LIMIT_BELOW_CONSUMED",
          "Daily limit cannot be lower than amount already consumed today.");
    }
    entity.setDailyLimit(dailyLimit);
    entity.setPerOperationLimit(operationLimit);
    return toPolicy(repository.save(entity), today);
  }

  @Transactional
  public TransferDecision validateAndConsume(
      String accountId, TransferChannel channel, BigDecimal amount) {
    LocalDate today = LocalDate.now(clock);
    AccountChannelLimitEntity entity =
        repository
            .findForUpdate(accountId, channel)
            .orElseThrow(() ->
                new LimitViolation(
                    "LIMITS_NOT_CONFIGURED",
                    "Limits are not configured for this account and channel."));
    BigDecimal consumed = currentUsage(entity, today);
    if (amount.compareTo(entity.getPerOperationLimit()) > 0) {
      throw new LimitViolation(
          "PER_OPERATION_LIMIT_EXCEEDED",
          render(OPERATION_REJECTED, amount, entity.getPerOperationLimit()));
    }
    BigDecimal remaining = entity.getDailyLimit().subtract(consumed);
    if (amount.compareTo(remaining) > 0) {
      throw new LimitViolation("DAILY_LIMIT_EXCEEDED", render(DAILY_REJECTED, amount, remaining));
    }
    entity.consume(amount, today);
    repository.save(entity);
    BigDecimal newConsumed = consumed.add(amount);
    return new TransferDecision(
        true,
        accountId,
        channel,
        amount,
        newConsumed,
        entity.getDailyLimit().subtract(newConsumed),
        Instant.now(clock),
        null);
  }

  private BigDecimal currentUsage(AccountChannelLimitEntity entity, LocalDate today) {
    return entity.getUsageDate().equals(today) ? entity.getConsumedAmount() : BigDecimal.ZERO;
  }

  private LimitPolicy toPolicy(AccountChannelLimitEntity entity, LocalDate today) {
    return new LimitPolicy(
        entity.getAccountId(),
        entity.getChannel(),
        entity.getDailyLimit(),
        entity.getPerOperationLimit(),
        currentUsage(entity, today),
        today);
  }

  private String render(String template, BigDecimal amount, BigDecimal limit) {
    return new StringSubstitutor(
            java.util.Map.of(
                "amount",
                amount.toPlainString(),
                "limit",
                limit.toPlainString(),
                "remaining",
                limit.toPlainString()))
        .replace(template);
  }
}
