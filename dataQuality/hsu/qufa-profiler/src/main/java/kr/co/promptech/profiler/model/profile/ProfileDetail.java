package kr.co.promptech.profiler.model.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * profile_detail 테이블과 매핑되는 엔티티 클래스
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
public class ProfileDetail {

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
     * row 개수
     */
    private Integer rowCnt;

    /**
     * NULL 개수
     */
    private Integer nullCnt;

    /**
     * 중복을 제외한 레코드 개수
     */
    private Integer distinctCnt;
    /**
     * 중복 레코드 개수
     */
    private Integer duplicateCnt;

    /**
     * 공백 레코드 개수
     */
    private Integer blankCnt;

    /**
     * 문자열 레코드 최소 길이
     */
    private Integer strMinLenVal;

    /**
     * 문자열 레코드 최대 길이
     */
    private Integer strMaxLenVal;

    /**
     * 문자열 레코드 평균 길이
     */
    private Double strAvgLenVal;

    /**
     * 숫자형 레코드 최소값
     */
    private Double numMinVal;

    /**
     * 숫자형 레코드 최대값
     */
    private Double numMaxVal;

    /**
     * 숫자형 레코드 중앙값
     */
    private Double numMedianVal;

    /**
     * 숫자형 레코드 평균값
     */
    private Double numMeanVal;

    /**
     * 최소 빈도 레코드 값
     */
    private String frquentMinVal;

    /**
     * 최다 빈도 레코드 값
     */
    private String frquentMaxVal;

    /**
     * profile_target_id (FK)
     */
    @JsonIgnore
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_target_id")
    private ProfileTarget profileTarget;
}
