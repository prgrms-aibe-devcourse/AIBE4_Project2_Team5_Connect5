package kr.eolmago.service.user;

import kr.eolmago.global.config.SupabaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileImageUploadService {

    private final SupabaseConfig supabaseConfig;

    /**
     * 프로필 이미지 업로드 (DB와 분리됨)
     * - 이 메서드는 @Transactional이 아님
     * - Supabase만 담당
     * - DB 연결을 점유하지 않음
     */
    public String uploadUserProfileImage(MultipartFile image, UUID userId) {
        log.debug("이미지 업로드 시작: fileName={}, size={}", image.getOriginalFilename(), image.getSize());

        validateImageFile(image);

        String imageUrl = uploadToSupabase(image, userId);

        log.info("이미지 업로드 완료: imageUrl={}", imageUrl);
        return imageUrl;
    }

    /**
     * 비동기 방식
     * - 이미지 업로드가 비동기로 진행됨
     * - DB 저장 후 바로 반환
     * - 사용자는 즉시 응답을 받음
     */
    @Async
    public CompletableFuture<String> uploadUserProfileImageAsync(
            MultipartFile image,
            UUID userId
    ) {
        log.debug("비동기 이미지 업로드 시작: userId={}", userId);

        try {
            String imageUrl = uploadToSupabase(image, userId);
            log.info("비동기 이미지 업로드 완료: userId={}, imageUrl={}", userId, imageUrl);
            return CompletableFuture.completedFuture(imageUrl);
        } catch (Exception e) {
            log.error("비동기 이미지 업로드 실패: userId={}", userId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void validateImageFile(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어있습니다");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다");
        }

        // 파일 크기 제한 (5MB)
        long maxSize = 5 * 1024 * 1024;
        if (image.getSize() > maxSize) {
            throw new IllegalArgumentException("파일 크기가 5MB를 초과합니다");
        }

        // 이미지 크기(가로/세로) 제한
        try {
            BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            if (width > 400 || height > 400) {
                throw new IllegalArgumentException("이미지 크기는 400x400을 초과할 수 없습니다.");
            }
        } catch (IOException e) {
            throw new RuntimeException("이미지 파일을 읽는 중 오류가 발생했습니다.", e);
        }


        log.debug("이미지 검증 통과: fileName={}, contentType={}", image.getOriginalFilename(), contentType);
    }

    private String uploadToSupabase(MultipartFile image, UUID userId) {
        try {
            // Supabase REST API 호출
            String fileName = generateUniqueFileName(image.getOriginalFilename(), userId);
            String uploadUrl = supabaseConfig.getUrl() + "/storage/v1/object/" +
                    supabaseConfig.getBucket() + "/" + fileName;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseConfig.getServiceRoleKey());
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("x-upsert", "true"); // 덮어쓰기 옵션

            byte[] fileContent = image.getBytes();
            HttpEntity<byte[]> entity = new HttpEntity<>(fileContent, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                String imageUrl = supabaseConfig.getUrl() + "/storage/v1/object/public/" +
                        supabaseConfig.getBucket() + "/" + fileName;
                log.info("Supabase 업로드 성공: fileName={}", fileName);
                return imageUrl;
            } else {
                throw new RuntimeException("Supabase 업로드 실패: " + response.getStatusCode());
            }
        } catch (IOException e) {
            log.error("파일 읽기 오류", e);
            throw new RuntimeException("파일 처리 중 오류 발생", e);
        }
    }

    private String generateUniqueFileName(String originalFileName, UUID userId) {
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return "user_profile/" + userId.toString() + "/profile" + extension;
    }
}