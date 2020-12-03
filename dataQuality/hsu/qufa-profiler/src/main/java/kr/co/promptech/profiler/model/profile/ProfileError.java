package kr.co.promptech.profiler.model.profile;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * profile_error 테이블과 매핑되는 엔티티 클래스
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
public class ProfileError {

    /**
     * 고유 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * profile_queue_id (FK)
     */
    @OneToOne
    @JoinColumn(name = "profile_queue_id")
    private ProfileQueue profileQueue;

    /**
     * 에러메세지
     */
    @Column(columnDefinition = "TEXT")
    private String errorMsg;

    /**
     * 생성일시
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
