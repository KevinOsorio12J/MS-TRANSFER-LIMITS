package com.kodea.transferlimits.api;

import com.kodea.transferlimits.application.LimitService;
import com.kodea.transferlimits.domain.LimitPolicy;
import com.kodea.transferlimits.domain.TransferChannel;
import com.kodea.transferlimits.domain.TransferDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class LimitController {
  private final LimitService service;

  public LimitController(LimitService service) {
    this.service = service;
  }

  @GetMapping("/limits")
  public List<LimitPolicy> getLimits(@PathVariable @NotBlank @Size(max = 64) String accountId) {
    return service.getLimits(accountId);
  }

  @PutMapping("/limits/{channelId}")
  public LimitPolicy updateLimit(
      @PathVariable @NotBlank @Size(max = 64) String accountId,
      @PathVariable TransferChannel channelId,
      @Valid @RequestBody UpdateLimitRequest request) {
    return service.updateLimit(
        accountId, channelId, request.dailyLimit(), request.perOperationLimit());
  }

  @PostMapping("/transfer-validations")
  @ResponseStatus(HttpStatus.OK)
  public TransferDecision validate(
      @PathVariable @NotBlank @Size(max = 64) String accountId,
      @Valid @RequestBody TransferRequest request) {
    return service.validateAndConsume(accountId, request.channel(), request.amount());
  }

  public record UpdateLimitRequest(
      @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2)
          BigDecimal dailyLimit,
      @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2)
          BigDecimal perOperationLimit) {}

  public record TransferRequest(
      @NotNull TransferChannel channel,
      @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {}
}
