package kr.eolmago.repository.report;

import kr.eolmago.domain.entity.report.Report;
import kr.eolmago.domain.entity.report.enums.ReportStatus;
import kr.eolmago.domain.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportRepositoryCustom {
    Page<Report> findAllWithDetails(Pageable pageable);
    Page<Report> findByReporterWithDetails(User reporter, Pageable pageable);
    Page<Report> findReportsWithFilters(ReportStatus status, Pageable pageable);
}
