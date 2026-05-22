package ptit.tmdt.lop6nhom7.baodientu.dto;

import lombok.Data;

@Data
public class ModerateArticleDecisionRequest {
  private boolean approved;
  private String rejectionReason;
}