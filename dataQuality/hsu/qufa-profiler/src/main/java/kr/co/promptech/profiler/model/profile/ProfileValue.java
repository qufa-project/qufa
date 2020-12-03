package kr.co.promptech.profiler.model.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * profile_value 테이블과 매핑되는 엔티티 클래스
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
public class ProfileValue {

    /**
     * 고유 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * 컬럼명
     */
    private String columnName;

    /**
     * 컬럼 코멘트 정보
     */
    private String columnDesc;

    /**
     * 그룹별 레코드값
     */
    private String columnGroupVal;

    /**
     * 그룹별 레코드 개수
     */
    private Integer columnGroupCount;

    /**
     * profile_target_id (FK)
     */
    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_target_id")
    private ProfileTarget profileTarget;
}
