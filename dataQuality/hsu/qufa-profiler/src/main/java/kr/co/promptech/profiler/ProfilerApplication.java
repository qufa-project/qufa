package kr.co.promptech.profiler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot 프로젝트 메인 클래스
 *
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ProfilerApplication {
    /**
     * 메소드 테스트용
     * @param args 파람 테스트
     */
    public static void main(String[] args) {

        SpringApplication.run(ProfilerApplication.class, args);
    }
}
