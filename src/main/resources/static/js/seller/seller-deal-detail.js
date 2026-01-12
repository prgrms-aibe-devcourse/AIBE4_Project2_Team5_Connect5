(function () {
    const root = document.getElementById('seller-deal-detail-root');
    if (!root) {
        console.error('seller-deal-detail-root를 찾을 수 없습니다.');
        return;
    }

    const dealId = root.dataset.dealId;

    // 상태 뱃지 스타일
    const statusConfig = {
        PENDING_CONFIRMATION: {
            label: '확정 대기',
            className: 'bg-yellow-100 text-yellow-800 border-yellow-300'
        },
        CONFIRMED: {
            label: '진행 중',
            className: 'bg-blue-100 text-blue-800 border-blue-300'
        },
        COMPLETED: {
            label: '완료',
            className: 'bg-green-100 text-green-800 border-green-300'
        },
        TERMINATED: {
            label: '취소',
            className: 'bg-red-100 text-red-800 border-red-300'
        },
        EXPIRED: {
            label: '만료',
            className: 'bg-gray-100 text-gray-800 border-gray-300'
        }
    };

    // 날짜 포맷
    function formatDate(dateString) {
        if (!dateString) return '-';
        try {
            const date = new Date(dateString);
            const y = date.getFullYear();
            const m = String(date.getMonth() + 1).padStart(2, '0');
            const d = String(date.getDate()).padStart(2, '0');
            const h = String(date.getHours()).padStart(2, '0');
            const min = String(date.getMinutes()).padStart(2, '0');
            return `${y}-${m}-${d} ${h}:${min}`;
        } catch (e) {
            return dateString;
        }
    }

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

    // 상세 데이터 렌더링
    function renderDetail(data) {
        // 기본 정보
        document.getElementById('deal-id').textContent = `거래 #${data.dealId}`;
        document.getElementById('deal-created-at').textContent = formatDate(data.createdAt);
        document.getElementById('deal-final-price').textContent =
            data.finalPrice != null
                ? `${Number(data.finalPrice).toLocaleString('ko-KR')}원`
                : '-';

        // 상태 뱃지
        const statusInfo = statusConfig[data.status] || {
            label: data.status || '알 수 없음',
            className: 'bg-gray-100 text-gray-800 border-gray-300'
        };
        const badge = document.getElementById('deal-status-badge');
        badge.textContent = statusInfo.label;
        badge.className =
            'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border ' +
            statusInfo.className;

        // 구매자 정보
        document.getElementById('buyer-nickname').textContent =
            data.buyerNickname || '-';
        
        // 기한 정보
        document.getElementById('confirm-by-at').textContent = formatDate(data.confirmByAt);
        document.getElementById('ship-by-at').textContent = formatDate(data.shipByAt);

        // 확정/완료 정보
        document.getElementById('seller-confirmed').textContent =
            data.sellerConfirmedAt ? '완료' : '대기';
        document.getElementById('buyer-confirmed').textContent =
            data.buyerConfirmedAt ? '완료' : '대기';
        document.getElementById('confirmed-at').textContent = formatDate(data.confirmedAt);
        document.getElementById('seller-confirmed-at').textContent = formatDate(data.sellerConfirmedAt);
        document.getElementById('buyer-confirmed-at').textContent = formatDate(data.buyerConfirmedAt);
        document.getElementById('completed-at').textContent = formatDate(data.completedAt);

        // 판매자 확정 버튼 활성/비활성
        const btn = document.getElementById('btn-seller-confirm');
        const canConfirm =
            data.status === 'PENDING_CONFIRMATION' && !data.sellerConfirmedAt;

        btn.disabled = !canConfirm;
        if (!canConfirm && data.status === 'PENDING_CONFIRMATION') {
            btn.textContent = '판매자 확정 완료';
        } else {
            btn.textContent = '판매자 확정하기';
        }
    }

    // 상세 데이터 호출
    async function loadDetail() {
        try {
            const res = await fetch(`/api/seller/deals/${dealId}`, {
                method: 'GET',
                credentials: 'include',
                headers: {
                    Accept: 'application/json'
                }
            });

            if (!res.ok) {
                if (res.status === 401) {
                    window.location.href = '/login';
                    return;
                }
                const errorData = await res.json();
                throw new Error(errorData.message || '상세 조회 실패');
            }

            const data = await res.json();
            renderDetail(data);
        } catch (e) {
            console.error(e);
            showToast(e.message, 'error');
        }
    }

    // 초기 로딩
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadDetail);
    } else {
        loadDetail();
    }
})();
