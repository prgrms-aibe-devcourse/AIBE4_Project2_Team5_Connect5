package kr.eolmago.repository.report.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.eolmago.domain.entity.report.Report;
import kr.eolmago.domain.entity.report.enums.ReportStatus;
import kr.eolmago.domain.entity.user.QUser;
import kr.eolmago.domain.entity.user.QUserProfile;
import kr.eolmago.domain.entity.user.User;
import kr.eolmago.repository.report.ReportRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static kr.eolmago.domain.entity.report.QReport.report;

@Repository
@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Report> findAllWithDetails(Pageable pageable) {
        QUser reporter = new QUser("reporter");
        QUserProfile reporterProfile = new QUserProfile("reporterProfile");
        QUser reportedUser = new QUser("reportedUser");
        QUserProfile reportedUserProfile = new QUserProfile("reportedUserProfile");

        List<Report> reports = queryFactory
                .selectFrom(report)
                .join(report.reporter, reporter).fetchJoin()
                .join(reporter.userProfile, reporterProfile).fetchJoin()
                .join(report.reportedUser, reportedUser).fetchJoin()
                .join(reportedUser.userProfile, reportedUserProfile).fetchJoin()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(report.createdAt.desc()) // 예시 정렬
                .fetch();

        long total = queryFactory
                .select(report.count())
                .from(report)
                .fetchOne();

        return new PageImpl<>(reports, pageable, total);
    }

    @Override
    public Page<Report> findByReporterWithDetails(User reporterUser, Pageable pageable) {
        QUser reporterAlias = new QUser("reporter");
        QUserProfile reporterProfileAlias = new QUserProfile("reporterProfile");
        QUser reportedUserAlias = new QUser("reportedUser");
        QUserProfile reportedUserProfileAlias = new QUserProfile("reportedUserProfile");

        List<Report> reports = queryFactory
                .selectFrom(report)
                .join(report.reporter, reporterAlias).fetchJoin()
                .join(reporterAlias.userProfile, reporterProfileAlias).fetchJoin()
                .join(report.reportedUser, reportedUserAlias).fetchJoin()
                .join(reportedUserAlias.userProfile, reportedUserProfileAlias).fetchJoin()
                .where(reporterAlias.userId.eq(reporterUser.getUserId()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(report.createdAt.desc())
                .fetch();

        long total = queryFactory
                .select(report.count())
                .from(report)
                .where(report.reporter.userId.eq(reporterUser.getUserId()))
                .fetchOne();

        return new PageImpl<>(reports, pageable, total);
    }

    @Override
    public Page<Report> findReportsWithFilters(ReportStatus status, Pageable pageable) {
        QUser reporter = new QUser("reporter");
        QUserProfile reporterProfile = new QUserProfile("reporterProfile");
        QUser reportedUser = new QUser("reportedUser");
        QUserProfile reportedUserProfile = new QUserProfile("reportedUserProfile");

        BooleanBuilder builder = new BooleanBuilder();
        if (status != null) {
            builder.and(report.status.eq(status));
        }

        List<Report> reports = queryFactory
                .selectFrom(report)
                .join(report.reporter, reporter).fetchJoin()
                .leftJoin(reporter.userProfile, reporterProfile).fetchJoin()
                .join(report.reportedUser, reportedUser).fetchJoin()
                .leftJoin(reportedUser.userProfile, reportedUserProfile).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(report.createdAt.desc())
                .fetch();

        long total = queryFactory
                .select(report.count())
                .from(report)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(reports, pageable, total);
    }
}
