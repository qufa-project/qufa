package kr.co.promptech.profiler.model.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * meta_column 테이블과 매핑되는 엔티티 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class MetaColumn {

    /**
     * 고유 id (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * 원본 컬럼 이름
     */
    private String originalColumnName;

    /**
     * meta_id (FK)
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "meta_id")
    private Meta meta;

    /**
     * 컬럼 이름
     */
    @Transient
    private String columnName;

    /**
     * 컬럼 데이터 타입
     */
    @Transient
    private String type;

    /**
     * 원본 데이터 타입
     */
    @Transient
    private String originalType;

    /**
     * 컬럼 순서
     */
    @Transient
    private int order;

    /**
     * hidden 여부
     */
    @Transient
    private boolean isHidden;

    /**
     * 검색 가능 여부
     */
    @Transient
    private boolean isSearchable;

    /**
     * 생성일시
     */
    @Transient
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @Transient
    private LocalDateTime updatedAt;

    /**
     * 빈 값 여부
     */
    @Transient
    private boolean isNullable;

    /**
     * 날짜 형식
     */
    @Transient
    private String dateFormat;

    /**
     * 컬럼 크기
     */
    @Transient
    private String size;
}
