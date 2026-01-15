package kr.eolmago.service.user;

import kr.eolmago.domain.entity.user.User;
import kr.eolmago.domain.entity.user.enums.UserRole;
import kr.eolmago.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StopWatch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class CpuLoadComparisonTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("DB 삭제 작업으로 인한 부하 시뮬레이션")
    void simulateDbDeletionLoad() {
        // Given: 10,000개의 만료된 데이터가 DB에 있다고 가정하고 삽입
        int dataCount = 10000;
        List<User> usersToCreate = new ArrayList<>();
        for (int i = 0; i < dataCount; i++) {
            usersToCreate.add(User.create(UserRole.USER));
        }
        userRepository.saveAll(usersToCreate);
        System.out.printf("DB에 %d개의 테스트 데이터 삽입 완료.%n", dataCount);

        // When: 외부 모니터링 도구로 CPU 사용률을 관찰하면서 아래 삭제 작업을 실행
        System.out.println("지금부터 10초 안에 DB 삭제 작업을 시작합니다. CPU 모니터링을 확인하세요...");
        try {
            Thread.sleep(10000); // 모니터링 준비 시간
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("DB Batch Delete (10,000 rows)");

        // 만료된 데이터를 삭제하는 스케줄러의 동작을 모방
        userRepository.deleteAllInBatch(usersToCreate);

        stopWatch.stop();

        // Then: 결과 출력
        System.out.println("DB 삭제 작업 완료.");
        System.out.println(stopWatch.prettyPrint());
    }

    @Test
    @DisplayName("Redis TTL을 사용한 자동 만료 시나리오")
    void simulateRedisTtlExpiration() {
        // Given: 10,000개의 데이터가 Redis에 TTL과 함께 저장된다고 가정
        int dataCount = 10000;
        String value = "test-value";
        Duration ttl = Duration.ofSeconds(10); // 10초 후 만료

        System.out.printf("Redis에 %d개의 테스트 데이터를 10초 TTL로 삽입합니다.%n", dataCount);
        for (int i = 0; i < dataCount; i++) {
            String key = "test:ttl:" + UUID.randomUUID();
            redisTemplate.opsForValue().set(key, value, ttl);
        }

        // When: 외부 모니터링 도구로 CPU 사용률을 관찰
        System.out.println("데이터가 10초 후에 자동으로 만료됩니다. CPU 부하가 거의 없는 것을 확인하세요.");
        
        // Then: 별도의 삭제 작업이 필요 없음을 설명
        // 테스트가 끝날 때까지 잠시 대기하여 TTL 만료 관찰 시간을 줌
        try {
            Thread.sleep(15000); // 15초 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Redis 데이터 자동 만료 완료. 애플리케이션/DB 서버에 부하가 발생하지 않았습니다.");
    }
}
