package kr.co.promptech.profiler.model.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * meta 테이블과 매핑되는 엔티티 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Meta {

    /**
     * 고유 id (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * 원천 데이터 제목
     */
    private String title;

    /**
     * 원천 데이터 상태 : <br>
     *     enum('default','download-scheduled','download-done','metaload-scheduled','meta-loaded','data_load_scheduled','loaded','failed')
     */
    @Transient
    private String status;

    /**
     * 파일 종류
     */
    @Transient
    private String dataType;

    /**
     * 원본 파일 이름
     */
    @Transient
    private String originalFileName;

    /**
     * 파일 url
     */
    @Transient
    private String remoteFilePath;

    /**
     * 파일 경로
     */
    @Transient
    private String filePath;

    /**
     * 인코딩 방식
     */
    @Transient
    private String encoding;

    /**
     * 확장자 명
     */
    @Transient
    private String extension;

    /**
     * 호스트
     */
    @Transient
    private String host;

    /**
     * 포트 번호
     */
    @Transient
    private String port;

    /**
     * DB 이름
     */
    @Transient
    private String db;

    /**
     * DB 사용자 이름
     */
    @Transient
    private String dbUser;

    /**
     * DB 비밀번호
     */
    @Transient
    private String pwd;

    /**
     * 테이블 이름
     */
    @Transient
    private String table;

    /**
     * 사용하는 DBMS 종류 : <br>
     *     enum('mysql','oracle','mariadb','postgres','cubrid')
     */
    @Transient
    private String dbms;

    /**
     * row 수
     */
    @Transient
    private int rowCounts;

    /**
     * skip할 row 수
     */
    @Transient
    private int skip;

    /**
     * 시트 번호
     */
    @Transient
    private int sheet;

    /**
     * 사용자 id
     */
    @Transient
    private int userId;

    /**
     * 참조 번호
     */
    @Transient
    private int stageId;

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
     * 원천 데이터 샘플
     */
    @Transient
    @Column(columnDefinition = "TEXT")
    private String sample;
}