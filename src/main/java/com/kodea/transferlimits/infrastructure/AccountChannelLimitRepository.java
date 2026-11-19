package com.kodea.transferlimits.infrastructure;

import com.kodea.transferlimits.domain.TransferChannel;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountChannelLimitRepository
    extends JpaRepository<AccountChannelLimitEntity, AccountChannelLimitKey> {
  List<AccountChannelLimitEntity> findByAccountIdOrderByChannel(String accountId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select l from AccountChannelLimitEntity l where l.accountId = :accountId and l.channel ="
          + " :channel")
  Optional<AccountChannelLimitEntity> findForUpdate(
      @Param("accountId") String accountId, @Param("channel") TransferChannel channel);
}
