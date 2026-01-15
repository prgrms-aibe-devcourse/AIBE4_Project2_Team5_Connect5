package kr.eolmago.service.deal;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import kr.eolmago.domain.entity.deal.Deal;
import kr.eolmago.domain.entity.deal.DealDocument;
import kr.eolmago.domain.entity.deal.enums.DealStatus;
import kr.eolmago.dto.api.deal.response.DealPdfDto;
import kr.eolmago.global.exception.BusinessException;
import kr.eolmago.global.exception.ErrorCode;
import kr.eolmago.repository.deal.DealDocumentRepository;
import kr.eolmago.repository.deal.DealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Deal PDF 생성 Service
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealPdfService {

    private final DealRepository dealRepository;
    private final DealDocumentRepository dealDocumentRepository;
    private final DealPdfStorageService pdfStorageService;

    private static final DateTimeFormatter DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분");
    
    private static final NumberFormat CURRENCY_FORMAT = 
        NumberFormat.getCurrencyInstance(Locale.KOREA);

    /**
     * 구매자용 PDF 생성 (COMPLETED 상태만 허용)
     */
    @Transactional
    public byte[] generatePdfForBuyer(Long dealId, java.util.UUID buyerId) {
        DealPdfDto pdfData = dealRepository.findPdfDataByDealId(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 권한 검증: 구매자 본인인지 확인
        if (!pdfData.buyerId().equals(buyerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        // 상태 검증: 구매자는 COMPLETED 상태에서만 다운로드 가능
        if (pdfData.status() != DealStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.DEAL_PDF_ONLY_COMPLETED);
        }

        return generatePdfInternal(dealId, pdfData);
    }

    /**
     * 판매자용 PDF 생성 (CONFIRMED 또는 COMPLETED 상태 허용)
     */
    @Transactional
    public byte[] generatePdfForSeller(Long dealId, java.util.UUID sellerId) {
        DealPdfDto pdfData = dealRepository.findPdfDataByDealId(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 권한 검증: 판매자 본인인지 확인
        if (!pdfData.sellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.DEAL_UNAUTHORIZED);
        }

        // 상태 검증: 판매자는 CONFIRMED 또는 COMPLETED 상태에서 다운로드 가능
        if (pdfData.status() != DealStatus.CONFIRMED && pdfData.status() != DealStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.DEAL_PDF_NOT_AVAILABLE);
        }

        return generatePdfInternal(dealId, pdfData);
    }

    /**
     * 거래확정서 PDF 생성 및 DealDocument 저장 (기존 메서드 유지 - 관리자용)
     */
    @Transactional
    public byte[] generateDealConfirmationPdf(Long dealId) {
        // PDF 생성용 데이터 조회 (한 번의 쿼리로 모든 필요한 데이터 조회)
        DealPdfDto pdfData = dealRepository.findPdfDataByDealId(dealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

        // 완료된 거래만 PDF 생성 가능
        if (pdfData.status() != DealStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.DEAL_PDF_ONLY_COMPLETED);
        }

        return generatePdfInternal(dealId, pdfData);
    }

    /**
     * 실제 PDF 생성 로직 (내부 메서드 - 중복 제거)
     */
    private byte[] generatePdfInternal(Long dealId, DealPdfDto pdfData) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // 한글 폰트 설정 (Nanum Gothic)
            byte[] fontBytes;
            try (java.io.InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/NanumGothic.ttf")) {
                if (fontStream == null) {
                    throw new RuntimeException("한글 폰트 파일을 찾을 수 없습니다: fonts/NanumGothic.ttf");
                }
                fontBytes = fontStream.readAllBytes();
            }

            PdfFont font = PdfFontFactory.createFont(
                fontBytes,
                PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED
            );

            // 제목
            Paragraph title = new Paragraph("거래확정서")
                    .setFont(font)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            document.add(new Paragraph("\n"));

            // 거래 정보 테이블
            Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .useAllAvailableWidth();

            addTableRow(table, font, "경매 ID", String.valueOf(pdfData.auctionId()));
            addTableRow(table, font, "상품명", pdfData.itemName());
            addTableRow(table, font, "판매자 이메일", pdfData.sellerEmail());
            addTableRow(table, font, "판매자 전화번호", pdfData.sellerPhoneNumber());
            addTableRow(table, font, "구매자 이메일", pdfData.buyerEmail());
            addTableRow(table, font, "구매자 전화번호", pdfData.buyerPhoneNumber());
            addTableRow(table, font, "최종 낙찰가", CURRENCY_FORMAT.format(pdfData.finalPrice()));
            addTableRow(table, font, "거래 상태", convertDealStatusToKorean(pdfData.status()));

            if (pdfData.completedAt() != null) {
                addTableRow(table, font, "거래 완료 일시",
                    pdfData.completedAt().plusHours(9).format(DATE_FORMAT));
            }

            if (pdfData.shippingNumber() != null) {
                addTableRow(table, font, "송장번호", pdfData.shippingNumber());
            }

            if (pdfData.shippingCarrierCode() != null) {
                addTableRow(table, font, "운송사 식별 코드", pdfData.shippingCarrierCode());
            }

            document.add(table);

            // 하단 서명란
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("이 문서는 거래가 성공적으로 완료되었음을 증명합니다.")
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("거래 확정서 생성 일시: " +
                java.time.OffsetDateTime.now().plusHours(9).format(DATE_FORMAT))
                    .setFont(font)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.RIGHT));

            document.close();

            // PDF 바이트 배열 생성
            byte[] pdfBytes = baos.toByteArray();

            // Deal 엔티티 조회 (DealDocument 저장용)
            Deal deal = dealRepository.findById(dealId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DEAL_NOT_FOUND));

            // DealDocument DB 저장 및 Supabase 업로드 (이미 존재하지 않는 경우에만)
            if (!dealDocumentRepository.existsByDeal(deal)) {
                String fileName = "deal-confirmation-" + dealId + ".pdf";
                long fileSizeBytes = pdfBytes.length;

                // 1. Supabase Storage에 PDF 업로드
                String supabasePdfUrl = pdfStorageService.uploadPdfToSupabase(pdfBytes, dealId, fileName);

                // 2. DealDocument DB 저장 (실제 Supabase URL 저장)
                DealDocument dealDocument = DealDocument.create(
                    deal,
                    supabasePdfUrl,  // Supabase Public URL
                    fileName,
                    fileSizeBytes
                );

                dealDocumentRepository.save(dealDocument);
            }

            return pdfBytes;

        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 중 오류가 발생했습니다", e);
        }
    }

    private void addTableRow(Table table, PdfFont font, String key, String value) {
        table.addCell(new Paragraph(key).setFont(font).setBold());
        // null 값은 "-"로 처리 (iText Paragraph는 null을 받을 수 없음)
        table.addCell(new Paragraph(value != null ? value : "-").setFont(font));
    }

    /**
     * DealStatus Enum을 한글로 변환
     */
    private String convertDealStatusToKorean(DealStatus status) {
        return switch (status) {
            case PENDING_CONFIRMATION -> "확인 대기";
            case CONFIRMED -> "확정됨";
            case COMPLETED -> "거래완료";
            case TERMINATED -> "종료됨";
            case EXPIRED -> "만료됨";
        };
    }
}
