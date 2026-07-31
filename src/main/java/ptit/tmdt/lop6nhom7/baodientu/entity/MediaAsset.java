package ptit.tmdt.lop6nhom7.baodientu.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "media_assets", schema = "pthttmdt")
public class MediaAsset {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "uploader_id", nullable = false)
  private Integer uploaderId;

  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "data", nullable = false, columnDefinition = "LONGBLOB")
  private byte[] data;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
