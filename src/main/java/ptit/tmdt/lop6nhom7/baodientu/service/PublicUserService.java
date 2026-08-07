package ptit.tmdt.lop6nhom7.baodientu.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import ptit.tmdt.lop6nhom7.baodientu.dto.PublicUserProfileResponse;
import ptit.tmdt.lop6nhom7.baodientu.dto.UserCommentActivityResponse;
import ptit.tmdt.lop6nhom7.baodientu.entity.User;
import ptit.tmdt.lop6nhom7.baodientu.enums.ArticleStatus;
import ptit.tmdt.lop6nhom7.baodientu.enums.UserStatus;
import ptit.tmdt.lop6nhom7.baodientu.exception.NotFoundException;
import ptit.tmdt.lop6nhom7.baodientu.repository.CommentRepo;
import ptit.tmdt.lop6nhom7.baodientu.repository.UserRepo;

@Service
@RequiredArgsConstructor
public class PublicUserService {

  private final UserRepo userRepo;
  private final CommentRepo commentRepo;

  @Transactional(readOnly = true)
  public PublicUserProfileResponse getPublicProfile(Integer userId) {
    User user = userRepo.findById(userId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

    if (user.getStatus() == UserStatus.LOCKED) {
      throw new NotFoundException("Không tìm thấy người dùng");
    }

    long commentCount = commentRepo.countByUserIdAndArticleStatus(userId, ArticleStatus.PUBLISHED);

    return PublicUserProfileResponse.builder()
        .id(user.getId())
        .displayName(user.getFullName())
        .avatarUrl(user.getAvatarUrl())
        .role(user.getRole().name())
        .commentCount(commentCount)
        .build();
  }

  @Transactional(readOnly = true)
  public Page<UserCommentActivityResponse> getUserComments(Integer userId, Pageable pageable) {
    User user = userRepo.findById(userId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

    if (user.getStatus() == UserStatus.LOCKED) {
      throw new NotFoundException("Không tìm thấy người dùng");
    }

    return commentRepo.findByUserIdAndArticleStatusOrderByCreatedAtDesc(userId, ArticleStatus.PUBLISHED, pageable)
        .map(comment -> UserCommentActivityResponse.builder()
            .commentId(comment.getId())
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .article(UserCommentActivityResponse.CommentArticleDto.builder()
                .id(comment.getArticle().getId())
                .title(comment.getArticle().getTitle())
                .categoryName(comment.getArticle().getCategory().getName())
                .thumbnailUrl(comment.getArticle().getCoverImage())
                .build())
            .build());
  }
}
