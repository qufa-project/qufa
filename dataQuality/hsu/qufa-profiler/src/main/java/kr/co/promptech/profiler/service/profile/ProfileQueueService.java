package kr.co.promptech.profiler.service.profile;

import kr.co.promptech.profiler.model.meta.Meta;
import kr.co.promptech.profiler.model.meta.MetaColumn;
import kr.co.promptech.profiler.model.profile.ProfileQueue;
import kr.co.promptech.profiler.model.profile.ProfileTarget;
import kr.co.promptech.profiler.reopository.profile.ProfileQueueRepository;
import kr.co.promptech.profiler.service.DataStoreService;
import kr.co.promptech.profiler.service.ProfileService;
import kr.co.promptech.profiler.service.meta.MetaColumnService;
import kr.co.promptech.profiler.service.meta.MetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * profile_queue(Job_queue)에 대한 서비스를 담당하는 클래스
 * 비동기 방식의 스케줄러 구현 클래스로, profile_queue에 등록된 작업을 통해 프로파일링을 수행한다.
 */
@Service
//@Slf4j
@RequiredArgsConstructor
public class ProfileQueueService {

    private final ProfileQueueRepository profileQueueRepository;
    private final ProfileErrorService profileErrorService;
    private final ProfileTargetService profileTargetService;
    private final MetaService metaService;
    private final MetaColumnService metaColumnService;
    private final ProfileService profileService;

    private ProfileTarget profileTarget;

    /**
     * 비동기 방식의 스케줄러 작동 메소드 <br>
     * 일정 시간마다 profile_queue를 탐색하여 프로파일링을 수행한다.
     */
    // 60000 ms = 1분, 작업 완료시간으로부터 타이머
    @Scheduled(fixedDelay = 60000)
    @Async
    @Transactional
    public void runProfiling() {
        System.out.println("\n----------------------------------Start profile scheduler----------------------------------\n");

        // test용 meta, meta_column, job queue insert
        this.makeTestTable();
        this.makeTestJobQueue();

        List<ProfileQueue> profileQueueList = profileQueueRepository.findAllByOrderByIdAsc();
        System.out.println("jobQueueList : " + profileQueueList);

        for (ProfileQueue profileQueue : profileQueueList) {
            if (profileQueue.getStatus().equals("wait") || profileQueue.getStatus().equals("failed")) {
                try {
                    // 해당 테이블 프로파일링 실행
                    profileQueue.setStatus("doing");
                    profileQueueRepository.save(profileQueue);

                    String targetTitle = profileQueue.getTargetTitle();
                    profileTarget = profileTargetService.createProfileTarget(targetTitle);

                    int targetId = metaService.getIdByTitle(targetTitle);
                    List<String> target_cols = metaColumnService.getColumnsByTargetId(targetId);

                    DataStoreService.createDataStore();
                    profileService.profileColumns(targetTitle, target_cols);

                    // profile success
                    profileQueueRepository.delete(profileQueue);
                    System.out.println("\n----------------------------------Done profiling by one table----------------------------------\n");
                } catch (Exception e) {
                    e.printStackTrace();

                    // profile_error 테이블에 에러메세지 저장.
                    profileQueue.setProfileError(profileErrorService.save(profileQueue, e.getMessage()));

                    // retry 횟수 2번 초과시 job_queue에서 삭제.
                    if (profileQueue.getRetryCnt() > 2 && profileQueue.getStatus().equals("failed")) {
                        profileQueueRepository.delete(profileQueue);
                    } else {
                        // profile_queue에 타겟 테이블 status = failed로 update
                        profileQueue.setStatus("failed");
                        profileQueue.setRetryCnt(profileQueue.getRetryCnt() + 1);
                        profileQueueRepository.save(profileQueue);
                    }
                    profileTargetService.delete(profileTarget);

                    System.out.println("\n----------------------------------Profiling failed----------------------------------\n");
                }
            }
        }
        System.out.println("----------------------------------Done sheduler----------------------------------");
    }

    // ---------------- 테스트용 method ------------------
    private void makeTestTable() {
//        Meta meta1 = new Meta();
//        meta1.setTitle("target_data");
//        metaService.save(meta1);
//        List<String> cols1 = metaService.getColumnsByTitle("target_data");
//        cols1.forEach(c -> {
//            MetaColumn metaColumn = new MetaColumn();
//            metaColumn.setOriginalColumnName(c);
//            metaColumn.setMeta(meta1);
//            metaColumn.setType(MetaColumn.TYPE.VARCHAR);
//            metaColumnService.save(metaColumn);
//        });
//
//        Meta meta2 = new Meta();
//        meta2.setTitle("test");
//        metaService.save(meta2);
//        List<String> cols2 = metaService.getColumnsByTitle("test");
//        cols2.forEach(c -> {
//            MetaColumn metaColumn = new MetaColumn();
//            metaColumn.setOriginalColumnName(c);
//            metaColumn.setMeta(meta2);
//            metaColumn.setType(MetaColumn.TYPE.VARCHAR);
//            metaColumnService.save(metaColumn);
//        });

        Meta meta3 = new Meta();
        meta3.setTitle("test_data");
        metaService.save(meta3);
        List<String> cols3 = metaService.getColumnsByTitle("test_data");
        cols3.forEach(c -> {
            MetaColumn metaColumn = new MetaColumn();
            metaColumn.setOriginalColumnName(c);
            metaColumn.setMeta(meta3);
            metaColumnService.save(metaColumn);
        });

    }

    private void makeTestJobQueue() {
        List<ProfileQueue> profileQueueList = new ArrayList<>();
        ProfileQueue profileQueue;
//        profileQueue = new ProfileQueue("target_data", "wait");
//        profileQueueList.add(profileQueue);
//        profileQueue = new ProfileQueue("test", "wait");
//        profileQueueList.add(profileQueue);
        profileQueue = new ProfileQueue("test_data", "wait");
        profileQueueList.add(profileQueue);

        profileQueueRepository.saveAll(profileQueueList);
    }
}
