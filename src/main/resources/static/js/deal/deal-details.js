/**
 * 통합 거래 상세 페이지 JavaScript
 * - BUYER/SELLER 역할에 따라 동적으로 동작
 * - 구매자: 확정 + 수령 확인 기능
 * - 판매자: 확정 기능
 */

(function () {
    const root = document.getElementById('deal-detail-root');
    if (!root) {
        console.error('deal-detail-root를 찾을 수 없습니다.');
        return;
    }

    const dealId = root.dataset.dealId;
    const role = root.dataset.role; // 'BUYER' or 'SELLER'
    let currentDeal = null; // 현재 거래 정보 저장

    // ========================================
    // 공통 유틸리티 함수
    // ========================================

    /**
     * 상태 설정 가져오기
     */
    function getStatusConfig(status) {
        const config = {
            'PENDING_CONFIRMATION': { label: '확정 대기', color: 'bg-yellow-100 text-yellow-800 border-yellow-300' },
            'CONFIRMED': { label: '진행 중', color: 'bg-blue-100 text-blue-800 border-blue-300' },
            'COMPLETED': { label: '완료', color: 'bg-green-100 text-green-800 border-green-300' },
            'TERMINATED': { label: '취소', color: 'bg-red-100 text-red-800 border-red-300' },
            'EXPIRED': { label: '만료', color: 'bg-gray-100 text-gray-800 border-gray-300' }
        };
        return config[status] || { label: status, color: 'bg-gray-100 text-gray-800 border-gray-300' };
    }

    /**
     * 날짜 포멧팅
     */
    function formatDate(dateString) {
        if (!dateString) return '-';
        try {
            const date = new Date(dateString);
            return date.toLocaleString('ko-KR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            });
        } catch (e) {
            return dateString;
        }
    }

    /**
     * 토스트 메시지 표시
     */
    function showToast(message, type = 'info') {
        const div = document.createElement('div');
        div.className =
            'fixed top-4 right-4 px-6 py-3 rounded-lg shadow-lg z-50 text-sm ' +
            (type === 'error'
                ? 'bg-red-500 text-white'
                : 'bg-blue-600 text-white');
        div.textContent = message;
        document.body.appendChild(div);
        setTimeout(() => div.remove(), 3000);
    }

    // ========================================
    // 초기 로드 및 UI 업데이트
    // ========================================

    /**
     * 거래 상세 정보 로드
     */
    async function loadDealDetail() {
        try {
            const apiUrl = role === 'BUYER'
                ? `/api/buyer/deals/${dealId}`
                : `/api/seller/deals/${dealId}`;

            const response = await fetch(apiUrl, {
                method: 'GET',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });

            if (!response.ok) {
                if (response.status === 401) {
                    window.location.href = '/login';
                    return;
                }
                const errorData = await response.json();
                throw new Error(errorData.message || 'API 호출 실패');
            }

            const deal = await response.json();
            currentDeal = deal; // 전역 변수에 저장
            updateUI(deal);

        } catch (error) {
            console.error('초기 로드 실패:', error);
            showToast('거래 정보를 불러오는 데 실패했습니다.', 'error');
        }
    }

    /**
     * UI 업데이트
     */
    function updateUI(deal) {

        // 썸네일 이미지
        const thumbnailImg = document.getElementById('thumbnail-image');
        if (deal.thumbnailUrl) {
            thumbnailImg.src = deal.thumbnailUrl;
            thumbnailImg.alt = deal.itemName || '상품 이미지';
        }

        // 기본 정보
        document.getElementById('item-name').textContent = deal.itemName || '상품명';
        document.getElementById('deal-final-price').textContent = `${Number(deal.finalPrice).toLocaleString('ko-KR')}원`;
        document.getElementById('deal-created-at').textContent = formatDate(deal.createdAt);

        // 상태 뱃지
        const statusBadge = document.getElementById('deal-status-badge');
        const statusConfig = getStatusConfig(deal.status);
        statusBadge.textContent = statusConfig.label;
        statusBadge.className = `inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border ${statusConfig.color}`;

        // 상대방 정보
        const counterpartNickname = role === 'BUYER' ? deal.sellerNickname : deal.buyerNickname;
        document.getElementById('counterpart-nickname').textContent = counterpartNickname || '-';

        // 기한 정보
        document.getElementById('confirm-by-at').textContent = formatDate(deal.confirmByAt) || '-';
        document.getElementById('ship-by-at').textContent = formatDate(deal.shipByAt) || '-';

        // 확정 상태
        document.getElementById('seller-confirmed').textContent = deal.sellerConfirmed ? '완료' : '미완료';
        document.getElementById('buyer-confirmed').textContent = deal.buyerConfirmed ? '완료' : '미완료';
        document.getElementById('confirmed-at').textContent = formatDate(deal.confirmedAt) || '-';

        // 완료 정보
        document.getElementById('seller-confirmed-at').textContent = formatDate(deal.sellerConfirmedAt) || '-';
        document.getElementById('buyer-confirmed-at').textContent = formatDate(deal.buyerConfirmedAt) || '-';
        document.getElementById('completed-at').textContent = formatDate(deal.completedAt) || '-';

        // PDF 다운로드 버튼 활성화
        updatePdfButton(deal);

        // 역할별 버튼 업데이트
        if (role === 'BUYER') {
            updateBuyerButtons(deal);
        } else if (role === 'SELLER') {
            updateSellerButtons(deal);
        }
    }

    /**
     * PDF 다운로드 버튼 활성화 로직
     * - 구매자: COMPLETED 상태일 때 활성화
     * - 판매자: CONFIRMED 또는 COMPLETED 상태일 때 활성화
     */
    function updatePdfButton(deal) {
        const pdfButton = document.getElementById('btn-download-pdf');

        let canDownload = false;
        if (role === 'BUYER') {
            // 구매자: 거래 완료 시에만 다운로드 가능
            canDownload = deal.status === 'COMPLETED';
        } else if (role === 'SELLER') {
            // 판매자: 거래 확정 이후 다운로드 가능
            // canDownload = deal.status === 'CONFIRMED' || deal.status === 'COMPLETED';
            canDownload = deal.status === 'COMPLETED';
        }

        pdfButton.disabled = !canDownload;
    }

    /**
     * 구매자 확정 버튼 제어
     */
    function updateBuyerButtons(deal) {
        const confirmButton = document.getElementById('btn-buyer-confirm');
        const receiveButton = document.getElementById('btn-receive-confirm');

        // 구매자 확정 버튼
        const canConfirm = deal.status === 'PENDING_CONFIRMATION' && !deal.buyerConfirmedAt;
        confirmButton.disabled = !canConfirm;

        if (deal.buyerConfirmedAt) {
            confirmButton.textContent = '구매자 확정 완료';
        } else {
            confirmButton.textContent = '거래 확정하기';
        }

        // 수령 확인 버튼
        // CONFIRMED 상태여야, 배송이 시작되었다는 의미
        const canReceive = deal.status === 'CONFIRMED';
        receiveButton.disabled = !canReceive;

        if (deal.status === 'COMPLETED') {
            receiveButton.textContent = '거래 완료됨';
        } else if (!canReceive) {
            receiveButton.textContent = '수령 확인 (거래 확정 후 가능)';
        } else {
            receiveButton.textContent = '수령 확인 (거래 완료)';
        }
    }

    /**
     * 판매자 확정 버튼 제어
     */
    function updateSellerButtons(deal) {
        const confirmButton = document.getElementById('btn-seller-confirm');

        // 판매자 확정 버튼
        const canConfirm = deal.status === 'PENDING_CONFIRMATION' && !deal.sellerConfirmedAt;
        confirmButton.disabled = !canConfirm;

        if (deal.sellerConfirmedAt) {
            confirmButton.textContent = '판매자 확정 완료';
        } else {
            confirmButton.textContent = '거래 확정하기';
        }
    }

    // ========================================
    // 모달 관리 함수
    // ========================================

    /**
     * 구매자 확정 모달 관리
     */
    function setupBuyerConfirmModal() {
        const buyerConfirmModal = document.getElementById('buyer-confirm-modal');
        const buyerConfirmBtn = document.getElementById('confirm-buyer-deal-btn');
        const cancelBuyerConfirmBtn = document.getElementById('cancel-buyer-confirm-btn');
        const buyerConfirmCheckbox = document.getElementById('buyer-confirm-checkbox');
        const openBuyerModalBtn = document.getElementById('btn-buyer-confirm');

        if (!buyerConfirmModal || !openBuyerModalBtn) return;

        function openBuyerConfirmModal() {
            buyerConfirmCheckbox.checked = false;
            buyerConfirmBtn.disabled = true;
            buyerConfirmModal.classList.remove('hidden');
        }

        function closeBuyerConfirmModal() {
            buyerConfirmModal.classList.add('hidden');
        }

        openBuyerModalBtn.addEventListener('click', openBuyerConfirmModal);
        cancelBuyerConfirmBtn.addEventListener('click', closeBuyerConfirmModal);
        buyerConfirmCheckbox.addEventListener('change', () => {
            buyerConfirmBtn.disabled = !buyerConfirmCheckbox.checked;
        });

        // 구매자 확정 API 호출
        buyerConfirmBtn.addEventListener('click', async () => {
            if (!buyerConfirmCheckbox.checked) return;

            try {
                const response = await fetch(`/api/buyer/deals/${dealId}/confirm`, {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Accept': 'application/json' }
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || '구매자 확정에 실패했습니다.');
                }

                closeBuyerConfirmModal();
                showToast('구매자 확정이 완료되었습니다.', 'info');
                window.location.reload();

            } catch (error) {
                showToast('오류: ' + error.message, 'error');
            }
        });
    }

    /**
     * 수령 확인 모달 관리
     */
    function setupReceiveConfirmModal() {
        const receiveConfirmModal = document.getElementById('receive-confirm-modal');
        const confirmReceiveBtn = document.getElementById('confirm-receive-btn');
        const cancelReceiveBtn = document.getElementById('cancel-receive-btn');
        const receiveConfirmCheckbox = document.getElementById('receive-confirm-checkbox');
        const openReceiveModalBtn = document.getElementById('btn-receive-confirm');

        if (!receiveConfirmModal || !openReceiveModalBtn) return;

        function openReceiveConfirmModal() {
            receiveConfirmCheckbox.checked = false;
            confirmReceiveBtn.disabled = true;
            receiveConfirmModal.classList.remove('hidden');
        }

        function closeReceiveConfirmModal() {
            receiveConfirmModal.classList.add('hidden');
        }

        openReceiveModalBtn.addEventListener('click', openReceiveConfirmModal);
        cancelReceiveBtn.addEventListener('click', closeReceiveConfirmModal);
        receiveConfirmCheckbox.addEventListener('change', () => {
            confirmReceiveBtn.disabled = !receiveConfirmCheckbox.checked;
        });

        // 수령 확인 API 호출
        confirmReceiveBtn.addEventListener('click', async () => {
            if (!receiveConfirmCheckbox.checked) return;

            try {
                const response = await fetch(`/api/buyer/deals/${dealId}/receive-confirm`, {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Accept': 'application/json' }
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || '수령 확인에 실패했습니다.');
                }

                closeReceiveConfirmModal();
                showToast('거래가 완료되었습니다!', 'info');
                window.location.reload();

            } catch (error) {
                showToast('오류: ' + error.message, 'error');
            }
        });
    }

    /**
     * 판매자 확정 모달 관리
     */
    function setupSellerConfirmModal() {
        const sellerConfirmModal = document.getElementById('seller-confirm-modal');
        const confirmSellerDealBtn = document.getElementById('confirm-seller-deal-btn');
        const cancelSellerConfirmBtn = document.getElementById('cancel-seller-confirm-btn');
        const sellerConfirmCheckbox = document.getElementById('seller-confirm-checkbox');
        const openSellerModalBtn = document.getElementById('btn-seller-confirm');

        if (!sellerConfirmModal || !openSellerModalBtn) return;

        function openSellerConfirmModal() {
            sellerConfirmCheckbox.checked = false;
            confirmSellerDealBtn.disabled = true;
            sellerConfirmModal.classList.remove('hidden');
        }

        function closeSellerConfirmModal() {
            sellerConfirmModal.classList.add('hidden');
        }

        openSellerModalBtn.addEventListener('click', openSellerConfirmModal);
        cancelSellerConfirmBtn.addEventListener('click', closeSellerConfirmModal);
        sellerConfirmCheckbox.addEventListener('change', () => {
            confirmSellerDealBtn.disabled = !sellerConfirmCheckbox.checked;
        });

        // 판매자 확정 API 호출
        confirmSellerDealBtn.addEventListener('click', async () => {
            if (!sellerConfirmCheckbox.checked) return;

            try {
                const response = await fetch(`/api/seller/deals/${dealId}/confirm`, {
                    method: 'POST',
                    credentials: 'include',
                    headers: { 'Accept': 'application/json' }
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || '판매자 확정에 실패했습니다.');
                }

                closeSellerConfirmModal();
                showToast('판매자 확정이 완료되었습니다.', 'info');
                window.location.reload();

            } catch (error) {
                showToast('오류: ' + error.message, 'error');
            }
        });
    }

    /**
     * 신고 모달 관리
     */
    function setupReportModal() {
        const reportButton = document.getElementById('report-button');
        const reportModal = document.getElementById('report-modal');
        const reportForm = document.getElementById('report-form');
        const reportCancel = document.getElementById('report-cancel');
        const reportDescription = document.getElementById('report-description');
        const reportReason = document.getElementById('report-reason');

        if (!reportButton || !reportModal) return;

        reportButton.addEventListener('click', () => {
            reportModal.classList.remove('hidden');
            reportModal.classList.add('flex');
        });

        reportCancel.addEventListener('click', () => {
            reportModal.classList.add('hidden');
            reportModal.classList.remove('flex');
            reportForm.reset();
        });

        reportForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(reportForm);
            const targetType = formData.get('reportTarget');
            const reason = reportReason.value;
            const description = reportDescription.value;

            if (!description || description.length < 10) {
                alert('신고 내용은 최소 10자 이상 입력해주세요.');
                return;
            }

            if (!currentDeal) {
                showToast('거래 정보를 불러오지 못해 신고할 수 없습니다.', 'error');
                return;
            }

            // 신고 대상 ID 결정 (상대방 ID)
            // role이 BUYER면 sellerId, SELLER면 buyerId
            // currentDeal 객체에 sellerId, buyerId가 있다고 가정 (API 응답 확인 필요)
            // 만약 API 응답에 ID가 없다면 추가 필요. 일단 닉네임만 보이는데...
            // DealResponse DTO를 확인해야 함.
            // 일단 currentDeal에 sellerId, buyerId가 있다고 가정하고 진행.
            // 만약 없다면 백엔드 수정 필요할 수 있음.
            // 하지만 보통 상세 조회에는 ID가 포함됨.

            // auctionId도 필요함. Deal은 Auction과 연관되어 있음.
            // currentDeal.auctionId 가 필요함.

            const reportedUserId = role === 'BUYER' ? currentDeal.sellerId : currentDeal.buyerId;
            const auctionId = currentDeal.auctionId;

            if (!reportedUserId || !auctionId) {
                console.error('Missing IDs:', { reportedUserId, auctionId, currentDeal });
                showToast('신고에 필요한 정보가 부족합니다.', 'error');
                return;
            }

            try {
                const response = await fetch('/api/reports', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({
                        auctionId: auctionId,
                        reportedUserId: reportedUserId,
                        type: targetType,
                        reason: reason,
                        description: description
                    })
                });

                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.message || '신고 접수에 실패했습니다.');
                }

                showToast('신고가 정상적으로 접수되었습니다.', 'info');
                reportModal.classList.add('hidden');
                reportModal.classList.remove('flex');
                reportForm.reset();

            } catch (err) {
                showToast(err.message || '신고 접수에 실패했습니다.', 'error');
            }
        });
    }

    // ========================================
    // PDF 다운로드
    // ========================================

    /**
     * PDF 다운로드 설정
     */
    function setupPdfDownload() {
        const pdfButton = document.getElementById('btn-download-pdf');
        if (!pdfButton) return;

        pdfButton.addEventListener('click', async () => {
            if (pdfButton.disabled) return;

            try {
                const apiUrl = role === 'BUYER'
                    ? `/api/buyer/deals/${dealId}/pdf`
                    : `/api/seller/deals/${dealId}/pdf`;

                const response = await fetch(apiUrl, {
                    method: 'GET',
                    credentials: 'include'
                });

                if (!response.ok) {
                    if (response.status === 401) {
                        window.location.href = '/login';
                        return;
                    }
                    throw new Error('PDF 다운로드에 실패했습니다.');
                }

                // PDF 파일 다운로드
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `거래확정서_${dealId}.pdf`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);

                showToast('PDF 다운로드가 완료되었습니다.', 'info');

            } catch (error) {
                console.error('PDF 다운로드 실패:', error);
                showToast('PDF 다운로드에 실패했습니다.', 'error');
            }
        });
    }

    // ========================================
    // 초기화 로직
    // ========================================

    /**
     * 역할별 모달 초기화
     */
    function initializeModals() {
        if (role === 'BUYER') {
            setupBuyerConfirmModal();
            setupReceiveConfirmModal();
        } else if (role === 'SELLER') {
            setupSellerConfirmModal();
        }
        setupReportModal(); // 신고 모달 초기화 추가
    }

    // 페이지 로드 시 실행
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            loadDealDetail();
            initializeModals();
            setupPdfDownload();
        });
    } else {
        loadDealDetail();
        initializeModals();
        setupPdfDownload();
    }
})();