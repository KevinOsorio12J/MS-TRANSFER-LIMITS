package com.kodea.transferlimits.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kodea.transferlimits.application.LimitService;
import com.kodea.transferlimits.domain.LimitPolicy;
import com.kodea.transferlimits.domain.TransferChannel;
import com.kodea.transferlimits.domain.TransferDecision;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LimitControllerTest {
  @Mock private LimitService service;
  @InjectMocks private LimitController controller;

  @Test
  void getsLimitsForAccount() {
    List<LimitPolicy> expected =
        List.of(
            new LimitPolicy(
                "acct",
                TransferChannel.WEB,
                new BigDecimal("100"),
                new BigDecimal("50"),
                BigDecimal.ZERO,
                LocalDate.of(2026, 11, 18)));
    when(service.getLimits("acct")).thenReturn(expected);
    assertEquals(expected, controller.getLimits("acct"));
  }

  @Test
  void updatesChannelLimits() {
    var body = new LimitController.UpdateLimitRequest(new BigDecimal("100"), new BigDecimal("50"));
    var expected =
        new LimitPolicy(
            "acct",
            TransferChannel.WEB,
            body.dailyLimit(),
            body.perOperationLimit(),
            BigDecimal.ZERO,
            LocalDate.of(2026, 11, 18));
    when(service.updateLimit(
            "acct", TransferChannel.WEB, body.dailyLimit(), body.perOperationLimit()))
        .thenReturn(expected);
    assertEquals(expected, controller.updateLimit("acct", TransferChannel.WEB, body));
  }

  @Test
  void validatesTransferRequest() {
    var body =
        new LimitController.TransferRequest(TransferChannel.MOBILE_APP, new BigDecimal("12.50"));
    var expected =
        new TransferDecision(
            true,
            "acct",
            body.channel(),
            body.amount(),
            body.amount(),
            new BigDecimal("87.50"),
            Instant.parse("2026-11-18T12:00:00Z"),
            null);
    when(service.validateAndConsume("acct", body.channel(), body.amount())).thenReturn(expected);
    assertEquals(expected, controller.validate("acct", body));
    verify(service).validateAndConsume("acct", body.channel(), body.amount());
  }
}
