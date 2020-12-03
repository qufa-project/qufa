package kr.co.promptech.profiler.model.profile;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * profile_queue 테이블과 매핑되는 엔티티 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class ProfileQueue {

    /**
     * 고유 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * 프로파일링 대상 테이블명
     */
    private String targetTitle;

    /**
     * 프로파일링 작업 상태 : <br>
     *     enum('wait', 'doing', 'failed')
     */
    private String status;

    /**
     * failed 작업 재시도 횟수
     */
    @Column(columnDefinition = "int default 0")
    private int retryCnt;

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

    /**
     * profile_error 테이블과 일대일 양방향 매핑
     */
    @OneToOne(mappedBy = "profileQueue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ProfileError profileError;

    @Builder
    public ProfileQueue(String targetTitle, String status) {
        this.targetTitle = targetTitle;
        this.status = status;
    }
}
