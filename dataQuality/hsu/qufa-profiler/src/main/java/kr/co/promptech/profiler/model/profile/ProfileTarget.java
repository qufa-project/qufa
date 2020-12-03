package kr.co.promptech.profiler.model.profile;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * profile_target 테이블과 매핑되는 엔티티 클래스
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
public class ProfileTarget {

    /**
     * 고유 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    /**
     * 프로파일링 대상 테이블명
     */
    private String title;

    /**
     * profile_detail 테이블과 일대다 양방향 매핑
     */
    @OneToMany(mappedBy = "profileTarget", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProfileDetail> profileDetails = new ArrayList<>();

    /**
     * profile_value 테이블과 일대다 양방향 매핑
     */
    @OneToMany(mappedBy = "profileTarget", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProfileValue> profileValues = new ArrayList<>();
}
