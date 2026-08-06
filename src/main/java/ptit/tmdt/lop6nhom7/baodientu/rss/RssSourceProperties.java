package ptit.tmdt.lop6nhom7.baodientu.rss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "news.rss")
public class RssSourceProperties {
    private boolean enabled = true;
    private List<FeedSource> sources = new ArrayList<>();

    @Data
    public static class FeedSource {
        private String name;
        private String url;
        private String defaultCategory;
    }
}
