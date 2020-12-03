package kr.co.promptech.profiler.reopository.profile;

import kr.co.promptech.profiler.model.profile.ProfileQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * profile_queue에 대한 JpaRepository 클래스
 */
@Repository
public interface ProfileQueueRepository extends JpaRepository<ProfileQueue, Integer> {

    List<ProfileQueue> findAllByOrderByIdAsc();
}
