package kr.eolmago.controller.api.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.eolmago.domain.entity.report.enums.ReportAction;
import kr.eolmago.dto.api.report.request.CreateReportRequest;
import kr.eolmago.dto.api.report.response.ReportResponse;
import kr.eolmago.global.security.CustomUserDetails;
import kr.eolmago.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Report", description = "신고 관련 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고 접수")
    @PostMapping
    public ResponseEntity<Long> createReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CreateReportRequest request
    ) {
        Long reportId = reportService.createReport(userDetails.getUserId(), request);
        return ResponseEntity.ok(reportId);
    }

    @Operation(summary = "내가 한 신고 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<Page<ReportResponse>> getMyReports(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault Pageable pageable
    ) {
        return ResponseEntity.ok(reportService.getMyReports(userDetails.getUserId(), pageable));
    }

    @Operation(summary = "신고 상세 조회")
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportResponse> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(reportService.getReportDetail(reportId));
    }

    @Operation(summary = "신고 목록 전체 조회 (관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ReportResponse>> getAllReports(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(reportService.getAllReports(pageable));
    }

    @Operation(summary = "신고 검토 (관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{reportId}/review")
    public ResponseEntity<Void> reviewReport(@PathVariable Long reportId) {
        reportService.reviewReport(reportId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "신고 처리-조치 (관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{reportId}/process")
    public ResponseEntity<Void> processReport(
            @PathVariable Long reportId,
            @RequestParam ReportAction action,
            @RequestParam(required = false) String memo
    ) {
        reportService.processReport(reportId, action, memo);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "신고 기각 (관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{reportId}/reject")
    public ResponseEntity<Void> rejectReport(
            @PathVariable Long reportId,
            @RequestParam(required = false) String memo
    ) {
        reportService.rejectReport(reportId, memo);
        return ResponseEntity.ok().build();
    }
}
