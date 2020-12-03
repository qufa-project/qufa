package kr.co.promptech.profiler.reopository.profile;

import kr.co.promptech.profiler.model.profile.ProfileTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * profile_target에 대한 JpaRepository 클래스
 */
@Repository
public interface ProfileTargetRepository extends JpaRepository<ProfileTarget, Integer> {
    ProfileTarget findByTitle(String title);
}
