package kr.co.promptech.profiler.reopository.profile;

import kr.co.promptech.profiler.model.profile.ProfileError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * profile_error에 대한 JpaRepository 클래스
 */
@Repository
public interface ProfileErrorRepository extends JpaRepository<ProfileError, Integer> {

    boolean existsByProfileQueueId(int id);

    ProfileError findByProfileQueueId(int id);

}
