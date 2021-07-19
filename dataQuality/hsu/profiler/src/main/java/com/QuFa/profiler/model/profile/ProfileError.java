package com.QuFa.profiler.model.profile;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * profile_error 테이블과 매핑되는 엔티티 클래스
 */
@NoArgsConstructor
@Getter
@Setter
public class ProfileError {

//    /**
//     * 고유 ID (PK)
//     */
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private int id;

    /**
     * 파일이름
     */
    //@Column(columnDefinition = "TEXT")
    private String filename;

    /**
     * 에러메세지
     */
    //@Column(columnDefinition = "TEXT")
    private String errorMsg;

//    /**
//     * 생성일시
//     */
//    @CreationTimestamp
//    private LocalDateTime createdAt;
//
//    /**
//     * 수정일시
//     */
//    @UpdateTimestamp
//    private LocalDateTime updatedAt;

}
