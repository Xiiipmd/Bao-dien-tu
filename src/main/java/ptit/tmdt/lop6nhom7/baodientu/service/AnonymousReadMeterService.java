package ptit.tmdt.lop6nhom7.baodientu.service;

import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class AnonymousReadMeterService {
  private static final int MONTHLY_FREE_LIMIT = 3;

  private final ConcurrentHashMap<String, ReaderWindow> readerWindows = new ConcurrentHashMap<>();

  public String createReaderKey() {
    return UUID.randomUUID().toString();
  }

  public synchronized MeteredAccessResult consumeRead(String readerKey, Integer articleId) {
    YearMonth currentMonth = YearMonth.now();
    ReaderWindow window = readerWindows.get(readerKey);
    if (window == null || !window.month().equals(currentMonth)) {
      window = new ReaderWindow(currentMonth, new HashSet<>());
      readerWindows.put(readerKey, window);
    }

    if (window.articleIds().contains(articleId)) {
      int remainingReads = Math.max(0, MONTHLY_FREE_LIMIT - window.articleIds().size());
      return new MeteredAccessResult(true, remainingReads, true, false);
    }

    if (window.articleIds().size() >= MONTHLY_FREE_LIMIT) {
      return new MeteredAccessResult(false, 0, false, false);
    }

    window.articleIds().add(articleId);
    int remainingReads = Math.max(0, MONTHLY_FREE_LIMIT - window.articleIds().size());
    return new MeteredAccessResult(true, remainingReads, false, true);
  }

  private record ReaderWindow(YearMonth month, Set<Integer> articleIds) {
  }

  public record MeteredAccessResult(boolean allowed, int remainingReads, boolean alreadyRead, boolean newlyConsumed) {
  }
}