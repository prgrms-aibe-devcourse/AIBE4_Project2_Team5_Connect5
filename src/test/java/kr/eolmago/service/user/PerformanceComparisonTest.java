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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class PerformanceComparisonTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("동시 접속자 100명 가정: DB vs Redis 성능 비교")
    void compareConcurrencyPerformance() throws InterruptedException {
        int userCount = 100; // 동시 접속자 수

        // 결과 출력을 위한 StopWatch
        StopWatch stopWatch = new StopWatch("Concurrency Performance Comparison");

        // --- 1. DB 동시성 테스트 ---
        // 100개의 스레드 풀 생성 (100명이 동시에 접속하는 환경 흉내)
        ExecutorService dbExecutor = Executors.newFixedThreadPool(userCount);
        CountDownLatch dbLatch = new CountDownLatch(userCount);

        // 테스트 후 삭제를 위해 저장된 ID를 모아둘 리스트 (동기화 필요)
        List<UUID> createdUserIds = Collections.synchronizedList(new ArrayList<>());

        stopWatch.start("DB: 100 Concurrent Requests");
        for (int i = 0; i < userCount; i++) {
            dbExecutor.submit(() -> {
                try {
                    // 1. 저장 (INSERT)
                    User user = User.create(UserRole.USER);
                    userRepository.save(user);
                    createdUserIds.add(user.getUserId());

                    // 2. 조회 (SELECT)
                    userRepository.findById(user.getUserId());
                } finally {
                    dbLatch.countDown(); // 작업 완료 카운트 감소
                }
            });
        }
        
        // 모든 스레드가 끝날 때까지 대기
        dbLatch.await(); 
        stopWatch.stop();
        
        // 리소스 정리
        dbExecutor.shutdown();
        userRepository.deleteAllById(createdUserIds);


        // --- 2. Redis 동시성 테스트 ---
        ExecutorService redisExecutor = Executors.newFixedThreadPool(userCount);
        CountDownLatch redisLatch = new CountDownLatch(userCount);
        
        List<String> createdKeys = Collections.synchronizedList(new ArrayList<>());

        stopWatch.start("Redis: 100 Concurrent Requests");
        for (int i = 0; i < userCount; i++) {
            redisExecutor.submit(() -> {
                try {
                    String key = "test:concurrent:" + UUID.randomUUID();
                    String value = "123456";
                    createdKeys.add(key);

                    // 1. 저장 (SET)
                    redisTemplate.opsForValue().set(key, value, 3, TimeUnit.MINUTES);

                    // 2. 조회 (GET)
                    redisTemplate.opsForValue().get(key);
                } finally {
                    redisLatch.countDown();
                }
            });
        }

        // 모든 스레드가 끝날 때까지 대기
        redisLatch.await();
        stopWatch.stop();

        // 리소스 정리
        redisExecutor.shutdown();
        redisTemplate.delete(createdKeys);


        // --- 결과 분석 및 출력 ---
        System.out.println("\n" + stopWatch.prettyPrint());

        double dbTime = stopWatch.getTaskInfo()[0].getTimeMillis();
        double redisTime = stopWatch.getTaskInfo()[1].getTimeMillis();

        System.out.println("==================================================");
        System.out.println("[테스트 시나리오: 100명의 유저가 동시에 인증 요청 (저장 + 조회)]");
        System.out.printf("DB    Total Time: %6.0f ms (평균 %.2f ms/req)%n", dbTime, dbTime / userCount);
        System.out.printf("Redis Total Time: %6.0f ms (평균 %.2f ms/req)%n", redisTime, redisTime / userCount);
        
        if (redisTime > 0) {
            double ratio = dbTime / redisTime;
            System.out.printf(">> Redis가 DB보다 약 %.1f배 빠릅니다.%n", ratio);
        }
        System.out.println("==================================================");
    }
}
