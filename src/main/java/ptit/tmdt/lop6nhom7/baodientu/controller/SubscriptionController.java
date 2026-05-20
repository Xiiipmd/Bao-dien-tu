package ptit.tmdt.lop6nhom7.baodientu.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import ptit.tmdt.lop6nhom7.baodientu.dto.SubscriptionRequest;
import ptit.tmdt.lop6nhom7.baodientu.dto.SubscriptionResponse;
import ptit.tmdt.lop6nhom7.baodientu.service.SubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Validated
public class SubscriptionController {
  private final SubscriptionService subscriptionService;

  @GetMapping("/my")
  public List<SubscriptionResponse> getMySubscriptions() {
    return subscriptionService.getMySubscriptions();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SubscriptionResponse subscribe(@RequestBody @Valid SubscriptionRequest request) {
    return subscriptionService.subscribe(request);
  }

  @DeleteMapping("/{targetType}/{targetId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unsubscribe(
      @PathVariable String targetType,
      @PathVariable @Positive Integer targetId
  ) {
    subscriptionService.unsubscribe(targetType, targetId);
  }
}
