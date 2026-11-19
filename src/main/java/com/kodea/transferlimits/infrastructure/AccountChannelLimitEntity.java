package com.kodea.transferlimits.infrastructure;

import com.kodea.transferlimits.domain.TransferChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "account_channel_limits")
@IdClass(AccountChannelLimitKey.class)
public class AccountChannelLimitEntity {
  @Id
  @Column(name = "account_id", nullable = false, length = 64)
  private String accountId;

  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 24)
  private TransferChannel channel;

  @Column(name = "daily_limit", nullable = false, precision = 19, scale = 2)
  private BigDecimal dailyLimit;

  @Column(name = "per_operation_limit", nullable = false, precision = 19, scale = 2)
  private BigDecimal perOperationLimit;

  @Column(name = "consumed_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal consumedAmount;

  @Column(name = "usage_date", nullable = false)
  private LocalDate usageDate;

  @Version private long version;

  protected AccountChannelLimitEntity() {}

  public AccountChannelLimitEntity(
      String accountId,
      TransferChannel channel,
      BigDecimal dailyLimit,
      BigDecimal perOperationLimit,
      BigDecimal consumedAmount,
      LocalDate usageDate) {
    this.accountId = accountId;
    this.channel = channel;
    this.dailyLimit = dailyLimit;
    this.perOperationLimit = perOperationLimit;
    this.consumedAmount = consumedAmount;
    this.usageDate = usageDate;
  }

  public String getAccountId() {
    return accountId;
  }

  public TransferChannel getChannel() {
    return channel;
  }

  public BigDecimal getDailyLimit() {
    return dailyLimit;
  }

  public BigDecimal getPerOperationLimit() {
    return perOperationLimit;
  }

  public BigDecimal getConsumedAmount() {
    return consumedAmount;
  }

  public LocalDate getUsageDate() {
    return usageDate;
  }

  public void setDailyLimit(BigDecimal value) {
    dailyLimit = value;
  }

  public void setPerOperationLimit(BigDecimal value) {
    perOperationLimit = value;
  }

  public void consume(BigDecimal amount, LocalDate today) {
    if (!usageDate.equals(today)) {
      usageDate = today;
      consumedAmount = BigDecimal.ZERO;
    }
    consumedAmount = consumedAmount.add(amount);
  }
}
