package backend.Board.repository;

import backend.Board.entity.Comment;
import backend.Board.entity.Like;
import backend.Board.entity.Post;
import backend.auth.entity.User;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // 게시글 좋아요 관련
    boolean existsByPostAndUser(Post post, User user);

    void deleteByPostAndUser(Post post, User user);

    // 댓글 좋아요 관련
    boolean existsByCommentAndUser(Comment comment, User user);

    void deleteByCommentAndUser(Comment comment, User user);

    @Query("SELECT l.post.id FROM Like l WHERE l.user.id = :userId AND l.post.id IN :postIds AND l.comment IS NULL")
    Set<Long> findLikedPostIdsByUserAndPosts(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);

    @Query("SELECT l.comment.id FROM Like l WHERE l.user.id = :userId AND l.comment.id IN :commentIds AND l.post IS NULL")
    Set<Long> findLikedCommentIdsByUserAndComments(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    // 사용자별 좋아요 조회
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
}