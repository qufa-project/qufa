package kr.co.promptech.profiler.service.profile;

import kr.co.promptech.profiler.model.profile.ProfileError;
import kr.co.promptech.profiler.model.profile.ProfileQueue;
import kr.co.promptech.profiler.reopository.profile.ProfileErrorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * profile_error에 대한 서비스를 담당하는 클래스
 */
@RequiredArgsConstructor
@Service
public class ProfileErrorService {

    private final ProfileErrorRepository profileErrorRepository;

//    public void save(ProfileError profileError) {
//        profileErrorRepository.save(profileError);
//    }

    public ProfileError save(ProfileQueue profileQueue, String errorMsg) {
        ProfileError profileError;

        if (profileErrorRepository.existsByProfileQueueId(profileQueue.getId()))
            profileError = profileErrorRepository.findByProfileQueueId(profileQueue.getId());
        else
            profileError = new ProfileError();

        profileError.setErrorMsg(errorMsg);
        profileError.setProfileQueue(profileQueue);
        profileErrorRepository.save(profileError);

        return profileError;
    }
}
