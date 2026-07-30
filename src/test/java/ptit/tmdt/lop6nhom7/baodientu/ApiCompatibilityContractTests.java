package ptit.tmdt.lop6nhom7.baodientu;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import ptit.tmdt.lop6nhom7.baodientu.dto.ArticlePreviewResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleReadResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.ArticleSearchResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.LoginRes;
import ptit.tmdt.lop6nhom7.baodientu.dto.VipPackageResponse;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserRole;
import ptit.tmdt.lop6nhom7.baodientu.enums.VipPreviewAccessMode;

class ApiCompatibilityContractTests {

  @Test
  void articleResponsesExposeFieldsRequiredByMobileApp() {
    ArticleSearchResponse search = ArticleSearchResponse.builder()
        .categoryId(2)
        .viewCount(15)
        .build();
    ArticleReadResponse read = ArticleReadResponse.builder()
        .authorId(4)
        .categoryId(2)
        .build();
    ArticlePreviewResponse preview = ArticlePreviewResponse.builder()
        .authorId(4)
        .categoryId(2)
        .accessMode(VipPreviewAccessMode.FREE_QUOTA)
        .remainingFreeReads(3)
        .willConsumeFreeRead(true)
        .build();

    assertAll(
        () -> assertEquals(2, search.getCategoryId()),
        () -> assertEquals(15, search.getViewCount()),
        () -> assertEquals(4, read.getAuthorId()),
        () -> assertEquals(2, read.getCategoryId()),
        () -> assertEquals(4, preview.getAuthorId()),
        () -> assertEquals(2, preview.getCategoryId()),
        () -> assertEquals(VipPreviewAccessMode.FREE_QUOTA, preview.getAccessMode()),
        () -> assertEquals(3, preview.getRemainingFreeReads()),
        () -> assertEquals(true, preview.isWillConsumeFreeRead())
    );
  }

  @Test
  void loginAndVipResponsesExposeFieldsRequiredByMobileApp() {
    LoginRes login = new LoginRes(
        "token",
        UserRole.MEMBER,
        "Doc gia",
        Instant.parse("2026-12-31T00:00:00Z"),
        3,
        9,
        "reader@example.com"
    );
    VipPackageResponse vipPackage = VipPackageResponse.builder()
        .price(new BigDecimal("59000"))
        .discountPercent(10)
        .build();

    assertAll(
        () -> assertEquals(9, login.getId()),
        () -> assertEquals("reader@example.com", login.getEmail()),
        () -> assertEquals(10, vipPackage.getDiscountPercent())
    );
  }
}
