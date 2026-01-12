(function () {
    const root = document.getElementById('buyer-deal-detail-root');
    if (!root) {
        console.error('buyer-deal-detail-root를 찾을 수 없습니다.');
        return;
    }
    const dealId = root.dataset.dealId;

    // 데이터 로드 및 화면 업데이트
    async function loadDealDetail() {
        try {
            const response = await fetch(`/api/buyer/deals/${dealId}`, {
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
            updateUI(deal);

        } catch (error) {
            console.error('데이터 로드 실패:', error);
            alert('거래 정보를 불러오는데 실패했습니다.');
        }
    }

    // UI 업데이트
    function updateUI(deal) {
        // 기본 정보
        document.getElementById('deal-id').textContent = `거래 #${deal.dealId}`;
        document.getElementById('deal-final-price').textContent = `${Number(deal.finalPrice).toLocaleString('ko-KR')}원`;
        document.getElementById('deal-created-at').textContent = formatDate(deal.createdAt);

        // 상태 배지
        const statusBadge = document.getElementById('deal-status-badge');
        const statusConfig = getStatusConfig(deal.status);
        statusBadge.textContent = statusConfig.label;
        statusBadge.className = `inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border ${statusConfig.color}`;

        // 판매자 정보
        document.getElementById('seller-nickname').textContent = deal.sellerNickname || '-';

        // 기한 정보
        document.getElementById('confirm-by-at').textContent = formatDate(deal.confirmByAt) || '-';
        document.getElementById('ship-by-at').textContent = formatDate(deal.shipByAt) || '-';

        // 확정 상태
        document.getElementById('seller-confirmed').textContent = deal.sellerConfirmedAt ? '완료' : '대기';
        document.getElementById('buyer-confirmed').textContent = deal.buyerConfirmedAt ? '완료' : '대기';
        document.getElementById('confirmed-at').textContent = formatDate(deal.confirmedAt) || '-';

        // 세부 시간
        document.getElementById('seller-confirmed-at').textContent = formatDate(deal.sellerConfirmedAt) || '-';
        document.getElementById('buyer-confirmed-at').textContent = formatDate(deal.buyerConfirmedAt) || '-';
        document.getElementById('completed-at').textContent = formatDate(deal.completedAt) || '-';

        // 구매자 확정 버튼 제어
        const confirmButton = document.getElementById('btn-buyer-confirm');
        const canConfirm = deal.status === 'PENDING_CONFIRMATION' && !deal.buyerConfirmedAt;
        
        confirmButton.disabled = !canConfirm;
        if (!canConfirm && deal.status === 'PENDING_CONFIRMATION') {
            confirmButton.textContent = '구매자 확정 완료';
        } else {
            confirmButton.textContent = '구매자 확정하기';
        }
    }

    // 상태 설정 가져오기
    function getStatusConfig(status) {
        const config = {
            'PENDING_CONFIRMATION': { label: '거래 대기', color: 'bg-yellow-100 text-yellow-800 border-yellow-300' },
            'CONFIRMED': { label: '진행 중', color: 'bg-blue-100 text-blue-800 border-blue-300' },
            'COMPLETED': { label: '완료', color: 'bg-green-100 text-green-800 border-green-300' },
            'TERMINATED': { label: '취소', color: 'bg-red-100 text-red-800 border-red-300' },
            'EXPIRED': { label: '만료', color: 'bg-gray-100 text-gray-800 border-gray-300' }
        };
        return config[status] || { label: status, color: 'bg-gray-100 text-gray-800 border-gray-300' };
    }

    // 날짜 포맷팅
    function formatDate(dateString) {
        if (!dateString) return '-';
        try {
            const date = new Date(dateString);
            return date.toLocaleString('ko-KR', {
                year: 'numeric', month: '2-digit', day: '2-digit',
                hour: '2-digit', minute: '2-digit', hour12: false
            });
        } catch (e) {
            return dateString;
        }
    }

    // 초기 데이터 로드
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadDealDetail);
    } else {
        loadDealDetail();
    }
})();
