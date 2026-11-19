package com.kodea.transferlimits.infrastructure;

import com.kodea.transferlimits.domain.TransferChannel;
import java.io.Serializable;
import java.util.Objects;

public class AccountChannelLimitKey implements Serializable {
  private String accountId;
  private TransferChannel channel;

  public AccountChannelLimitKey() {}

  public AccountChannelLimitKey(String accountId, TransferChannel channel) {
    this.accountId = accountId;
    this.channel = channel;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof AccountChannelLimitKey that)) return false;
    return Objects.equals(accountId, that.accountId) && channel == that.channel;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountId, channel);
  }
}
