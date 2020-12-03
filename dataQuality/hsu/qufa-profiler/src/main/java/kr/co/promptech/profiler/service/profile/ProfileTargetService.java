package kr.co.promptech.profiler.service.profile;

import kr.co.promptech.profiler.model.profile.ProfileDetail;
import kr.co.promptech.profiler.model.profile.ProfileTarget;
import kr.co.promptech.profiler.model.profile.ProfileValue;
import kr.co.promptech.profiler.reopository.profile.ProfileTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * profile_target에 대한 서비스를 수행하는 클래스
 */
@RequiredArgsConstructor
@Service
public class ProfileTargetService {

    private final ProfileTargetRepository profileTargetRepository;

    public ProfileTarget createProfileTarget(String title) {
        ProfileTarget profileTarget = new ProfileTarget();
        profileTarget.setTitle(title);
        profileTargetRepository.save(profileTarget);

        return profileTarget;

    }

    public void addProfileDetail(ProfileDetail profileDetail, String title) {
        ProfileTarget profileTarget = profileTargetRepository.findByTitle(title);
        profileTarget.getProfileDetails().add(profileDetail);
        profileDetail.setProfileTarget(profileTarget);
//        profileTarget.addProfileDetail(profileDetail);
        profileTargetRepository.save(profileTarget);
    }

    public void addProfileValue(List<ProfileValue> profileValues, String title) {
        ProfileTarget profileTarget = profileTargetRepository.findByTitle(title);
        for (ProfileValue profileValue : profileValues) {
//            profileTarget.addProfileValue(profileValue);
            profileTarget.getProfileValues().add(profileValue);
            profileValue.setProfileTarget(profileTarget);
        }
        profileTargetRepository.save(profileTarget);
    }

    public void delete(ProfileTarget profileTarget) {
        profileTargetRepository.delete(profileTarget);
    }
}
