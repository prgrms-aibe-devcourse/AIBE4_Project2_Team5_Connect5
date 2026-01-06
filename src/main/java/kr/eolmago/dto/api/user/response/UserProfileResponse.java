package kr.eolmago.dto.api.user.response;

import kr.eolmago.domain.entity.user.UserProfile;

import java.math.BigDecimal;
import java.util.UUID;

public record UserProfileResponse(
        Long profileId,
        UUID userId,
        String name,
        String nickname,
        String phoneNumber,
        boolean phoneVerified,
        String profileImageUrl,
        BigDecimal ratingAvg,
        int tradeCount,
        short reportCount
) {
    public static UserProfileResponse from(UserProfile userProfile) {
        return new UserProfileResponse(
                userProfile.getProfileId(),
                userProfile.getUser().getUserId(),
                userProfile.getName(),
                userProfile.getNickname(),
                userProfile.getPhoneNumber(),
                userProfile.isPhoneVerified(),
                userProfile.getProfileImageUrl(),
                userProfile.getRatingAvg(),
                userProfile.getTradeCount(),
                userProfile.getReportCount()
        );
    }
}
