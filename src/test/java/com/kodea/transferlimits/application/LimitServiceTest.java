package com.kodea.transferlimits.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kodea.transferlimits.domain.LimitPolicy;
import com.kodea.transferlimits.domain.LimitViolation;
import com.kodea.transferlimits.domain.TransferChannel;
import com.kodea.transferlimits.infrastructure.AccountChannelLimitEntity;
import com.kodea.transferlimits.infrastructure.AccountChannelLimitKey;
import com.kodea.transferlimits.infrastructure.AccountChannelLimitRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LimitServiceTest {
  private static final String ACCOUNT = "acct-123";
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);
  @Mock private AccountChannelLimitRepository repository;
  private LimitService service;

  @BeforeEach
  void setUp() {
    service =
        new LimitService(
            repository, Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void getLimitsReturnsEmptyWhenAccountHasNoLimits() {
    when(repository.findByAccountIdOrderByChannel(ACCOUNT)).thenReturn(List.of());
    assertTrue(service.getLimits(ACCOUNT).isEmpty());
  }

  @Test
  void getLimitsTreatsPreviousDayUsageAsZero() {
    AccountChannelLimitEntity entity =
        entity(
            new BigDecimal("1000"),
            new BigDecimal("500"),
            new BigDecimal("400"),
            TODAY.minusDays(1));
    when(repository.findByAccountIdOrderByChannel(ACCOUNT)).thenReturn(List.of(entity));
    List<LimitPolicy> policies = service.getLimits(ACCOUNT);
    assertEquals(BigDecimal.ZERO, policies.getFirst().consumedToday());
  }

  @Test
  void updateLimitCreatesPolicy() {
    when(repository.findById(any(AccountChannelLimitKey.class))).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    LimitPolicy policy =
        service.updateLimit(
            ACCOUNT, TransferChannel.WEB, new BigDecimal("1200"), new BigDecimal("600"));
    assertEquals(new BigDecimal("1200"), policy.dailyLimit());
    assertEquals(BigDecimal.ZERO, policy.consumedToday());
  }

  @Test
  void updateLimitRejectsDailyLimitBelowExistingConsumption() {
    when(repository.findById(any(AccountChannelLimitKey.class)))
        .thenReturn(
            Optional.of(
                entity(
                    new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("400"), TODAY)));
    LimitViolation error =
        assertThrows(
            LimitViolation.class,
            () ->
                service.updateLimit(
                    ACCOUNT, TransferChannel.WEB, new BigDecimal("300"), new BigDecimal("200")));
    assertEquals("DAILY_LIMIT_BELOW_CONSUMED", error.code());
  }

  @Test
  void acceptsTransferAndConsumesDailyLimit() {
    AccountChannelLimitEntity entity =
        entity(new BigDecimal("1000"), new BigDecimal("600"), new BigDecimal("100"), TODAY);
    when(repository.findForUpdate(ACCOUNT, TransferChannel.MOBILE_APP))
        .thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);
    var decision =
        service.validateAndConsume(ACCOUNT, TransferChannel.MOBILE_APP, new BigDecimal("250"));
    assertTrue(decision.approved());
    assertEquals(new BigDecimal("350"), decision.consumedToday());
    assertEquals(new BigDecimal("650"), decision.remainingToday());
  }

  @Test
  void resetsUsageOnFirstTransferOfNewDay() {
    AccountChannelLimitEntity entity =
        entity(
            new BigDecimal("1000"),
            new BigDecimal("600"),
            new BigDecimal("900"),
            TODAY.minusDays(1));
    when(repository.findForUpdate(ACCOUNT, TransferChannel.MOBILE_APP))
        .thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);
    var decision =
        service.validateAndConsume(ACCOUNT, TransferChannel.MOBILE_APP, new BigDecimal("250"));
    assertEquals(new BigDecimal("250"), decision.consumedToday());
    assertEquals(TODAY, entity.getUsageDate());
  }

  @Test
  void rejectsTransferAbovePerOperationLimitWithoutSaving() {
    AccountChannelLimitEntity entity =
        entity(new BigDecimal("1000"), new BigDecimal("200"), BigDecimal.ZERO, TODAY);
    when(repository.findForUpdate(ACCOUNT, TransferChannel.MOBILE_APP))
        .thenReturn(Optional.of(entity));
    LimitViolation error =
        assertThrows(
            LimitViolation.class,
            () ->
                service.validateAndConsume(
                    ACCOUNT, TransferChannel.MOBILE_APP, new BigDecimal("250")));
    assertEquals("PER_OPERATION_LIMIT_EXCEEDED", error.code());
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsTransferAboveRemainingDailyLimitWithoutSaving() {
    AccountChannelLimitEntity entity =
        entity(new BigDecimal("1000"), new BigDecimal("600"), new BigDecimal("850"), TODAY);
    when(repository.findForUpdate(ACCOUNT, TransferChannel.MOBILE_APP))
        .thenReturn(Optional.of(entity));
    LimitViolation error =
        assertThrows(
            LimitViolation.class,
            () ->
                service.validateAndConsume(
                    ACCOUNT, TransferChannel.MOBILE_APP, new BigDecimal("200")));
    assertEquals("DAILY_LIMIT_EXCEEDED", error.code());
    assertTrue(error.getMessage().contains("remaining daily limit 150"));
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsTransferWhenLimitsDoNotExist() {
    when(repository.findForUpdate(ACCOUNT, TransferChannel.BRANCH)).thenReturn(Optional.empty());
    LimitViolation error =
        assertThrows(
            LimitViolation.class,
            () -> service.validateAndConsume(ACCOUNT, TransferChannel.BRANCH, new BigDecimal("1")));
    assertEquals("LIMITS_NOT_CONFIGURED", error.code());
  }

  private AccountChannelLimitEntity entity(
      BigDecimal daily, BigDecimal operation, BigDecimal consumed, LocalDate date) {
    return new AccountChannelLimitEntity(
        ACCOUNT, TransferChannel.MOBILE_APP, daily, operation, consumed, date);
  }
}
