package ptit.tmdt.lop6nhom7.baodientu.service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import ptit.tmdt.lop6nhom7.baodientu.dto.utility.ExchangeRateDto;
import ptit.tmdt.lop6nhom7.baodientu.dto.utility.UtilityDataEnvelope;

@Service
public class ExchangeRateService {
    private static final String SOURCE = "Vietcombank";
    private static final String SOURCE_URL =
            "https://portal.vietcombank.com.vn/Usercontrols/TVPortal.TyGia/pXML.aspx";
    private static final Map<String, String> VIETNAMESE_NAMES = Map.ofEntries(
            Map.entry("AUD", "Đô la Úc"),
            Map.entry("CAD", "Đô la Canada"),
            Map.entry("CHF", "Franc Thụy Sĩ"),
            Map.entry("CNY", "Nhân dân tệ"),
            Map.entry("DKK", "Krone Đan Mạch"),
            Map.entry("EUR", "Euro"),
            Map.entry("GBP", "Bảng Anh"),
            Map.entry("HKD", "Đô la Hồng Kông"),
            Map.entry("INR", "Rupee Ấn Độ"),
            Map.entry("JPY", "Yên Nhật"),
            Map.entry("KRW", "Won Hàn Quốc"),
            Map.entry("KWD", "Dinar Kuwait"),
            Map.entry("MYR", "Ringgit Malaysia"),
            Map.entry("NOK", "Krone Na Uy"),
            Map.entry("RUB", "Rúp Nga"),
            Map.entry("SAR", "Riyal Ả Rập Xê Út"),
            Map.entry("SEK", "Krona Thụy Điển"),
            Map.entry("SGD", "Đô la Singapore"),
            Map.entry("THB", "Baht Thái Lan"),
            Map.entry("USD", "Đô la Mỹ"));

    private final RestClient client = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "NewsDaily/1.0")
            .build();

    public UtilityDataEnvelope<List<ExchangeRateDto>> getRates() {
        String xml = client.get().uri(SOURCE_URL).retrieve().body(String.class);
        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("Vietcombank không trả dữ liệu tỷ giá");
        }
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            var rateNodes = document.getElementsByTagName("Exrate");
            List<ExchangeRateDto> rates = new ArrayList<>();
            for (int index = 0; index < rateNodes.getLength(); index++) {
                Element item = (Element) rateNodes.item(index);
                String code = item.getAttribute("CurrencyCode").trim();
                rates.add(new ExchangeRateDto(
                        code,
                        VIETNAMESE_NAMES.getOrDefault(
                                code,
                                item.getAttribute("CurrencyName").trim()),
                        number(item.getAttribute("Buy")),
                        number(item.getAttribute("Transfer")),
                        number(item.getAttribute("Sell"))));
            }
            String providerTime = document.getElementsByTagName("DateTime").getLength() > 0
                    ? document.getElementsByTagName("DateTime").item(0).getTextContent().trim()
                    : "";
            String updatedAt = normalizeProviderTime(providerTime);
            return new UtilityDataEnvelope<>(rates, SOURCE, updatedAt);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể đọc dữ liệu tỷ giá Vietcombank", exception);
        }
    }

    private BigDecimal number(String value) {
        if (value == null || value.isBlank() || "-".equals(value.trim())) return null;
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeProviderTime(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                    "M/d/yyyy h:mm:ss a",
                    Locale.ENGLISH);
            return LocalDateTime.parse(value, formatter)
                    .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                    .toOffsetDateTime()
                    .toString();
        } catch (RuntimeException ignored) {
            return value;
        }
    }
}
