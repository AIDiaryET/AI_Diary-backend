package backend.Board.repository;

import backend.Board.dto.CommentResponse;
import backend.Board.entity.Comment;
import backend.Board.entity.Post;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 게시글의 최상위 댓글 조회
    List<Comment> findByPostAndParentIsNullOrderByCreatedAtAsc(Post post);

    // 대댓글 조회
    List<Comment> findByParentOrderByCreatedAtAsc(Comment parent);

    // Repository에 추가할 최적화 쿼리
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.replies " +
            "WHERE c.post = :post AND c.parent IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findByPostWithReplies(@Param("post") Post post);

    // 게시글의 모든 댓글 수 조회
    long countByPost(Post post);
}