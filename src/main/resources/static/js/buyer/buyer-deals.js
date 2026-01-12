(function () {
    let allDeals = []; // 전체 거래 데이터
    let currentTab = 'all'; // 현재 선택된 탭 (기본: 전체)
    let selectedDealId = null; // 모달에서 사용할 거래 ID

    // 모달 요소
    const modal = document.getElementById('buyer-confirm-modal');
    const confirmDealBtn = document.getElementById('confirm-deal-btn');
    const cancelConfirmBtn = document.getElementById('cancel-confirm-btn');
    const confirmCheckbox = document.getElementById('buyer-confirm-checkbox');

    // 탭 전환
    const tabButtons = document.querySelectorAll('.buyer-deal-tab-btn');
    const tabContents = document.querySelectorAll('.buyer-deal-tab-content');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.dataset.tab;
            currentTab = tabName;

            tabButtons.forEach(b => {
                b.classList.remove('border-gray-900', 'text-gray-900', 'font-semibold');
                b.classList.add('border-transparent', 'text-gray-500', 'font-medium');
            });

            btn.classList.remove('border-transparent', 'text-gray-500', 'font-medium');
            btn.classList.add('border-gray-900', 'text-gray-900', 'font-semibold');

            tabContents.forEach(content => {
                content.classList.add('hidden');
            });

            const targetContent = document.getElementById(`tab-${tabName}`);
            if (targetContent) {
                targetContent.classList.remove('hidden');
            }

            displayFilteredDeals(tabName);
        });
    });

    // 데이터 로드
    async function loadDeals() {
        try {
            const response = await fetch('/api/buyer/deals', {
                method: 'GET',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });

            if (!response.ok) {
                if (response.status === 401) window.location.href = '/login';
                throw new Error('API 호출 실패: ' + response.status);
            }

            const data = await response.json();
            allDeals = data.deals || [];
            updateTabCounts(allDeals);
            displayFilteredDeals(currentTab);

        } catch (error) {
            showError('데이터를 불러오는데 실패했습니다: ' + error.message);
        }
    }

    // 탭 카운트 업데이트
    function updateTabCounts(deals) {
        const counts = { all: deals.length, pending: 0, ongoing: 0, completed: 0, cancelled: 0 };
        deals.forEach(deal => {
            const status = deal.status;
            if (status === 'PENDING_CONFIRMATION') counts.pending++;
            else if (status === 'CONFIRMED') counts.ongoing++;
            else if (status === 'COMPLETED') counts.completed++;
            else if (status === 'TERMINATED' || status === 'EXPIRED') counts.cancelled++;
        });
        document.getElementById('all-count').textContent = counts.all;
        document.getElementById('pending-count').textContent = counts.pending;
        document.getElementById('ongoing-count').textContent = counts.ongoing;
        document.getElementById('completed-count').textContent = counts.completed;
        document.getElementById('cancelled-count').textContent = counts.cancelled;
    }

    // 필터링된 거래 표시
    function displayFilteredDeals(tabName) {
        let filteredDeals = [];
        let containerSelector = '';
        let emptyMessage = '';

        switch(tabName) {
            case 'all':
                filteredDeals = allDeals;
                containerSelector = '#tab-all .space-y-4';
                emptyMessage = '거래가 없습니다';
                break;
            case 'pending':
                filteredDeals = allDeals.filter(d => d.status === 'PENDING_CONFIRMATION');
                containerSelector = '#tab-pending .space-y-4';
                emptyMessage = '거래 대기 중인 거래가 없습니다';
                break;
            case 'ongoing':
                filteredDeals = allDeals.filter(d => d.status === 'CONFIRMED');
                containerSelector = '#tab-ongoing .space-y-4';
                emptyMessage = '진행 중인 거래가 없습니다';
                break;
            case 'completed':
                filteredDeals = allDeals.filter(d => d.status === 'COMPLETED');
                containerSelector = '#tab-completed .space-y-4';
                emptyMessage = '완료된 거래가 없습니다';
                break;
            case 'cancelled':
                filteredDeals = allDeals.filter(d => d.status === 'TERMINATED' || d.status === 'EXPIRED');
                containerSelector = '#tab-cancelled .space-y-4';
                emptyMessage = '취소/만료된 거래가 없습니다';
                break;
        }
        
        const container = document.querySelector(containerSelector);
        if (filteredDeals.length === 0) {
            container.innerHTML = `<div class="text-center py-12 bg-gray-50 rounded-lg"><p>${emptyMessage}</p></div>`;
            return;
        }
        container.innerHTML = filteredDeals.map(deal => createDealCard(deal)).join('');
    }

    // 거래 카드 생성
    function createDealCard(deal) {
        const statusConfig = {
            'PENDING_CONFIRMATION': { label: '거래 대기', color: 'bg-yellow-100 text-yellow-800' },
            'CONFIRMED': { label: '진행 중', color: 'bg-blue-100 text-blue-800' },
            'COMPLETED': { label: '완료', color: 'bg-green-100 text-green-800' },
            'TERMINATED': { label: '취소', color: 'bg-red-100 text-red-800' },
            'EXPIRED': { label: '만료', color: 'bg-gray-100 text-gray-800' }
        };
        const status = statusConfig[deal.status] || { label: deal.status, color: 'bg-gray-100' };

        let actionButtons = '';
        if (deal.status === 'PENDING_CONFIRMATION') {
            if (deal.buyerConfirmedAt) {
                // 구매자가 이미 확정한 경우
                actionButtons = `
                    <div class="flex-1 text-center text-sm font-medium text-green-600">
                        구매 확정됨
                    </div>
                `;
            } else {
                // 구매자가 아직 확정하지 않은 경우
                actionButtons = `
                    <button onclick="openConfirmModal(${deal.dealId})"
                            class="flex-1 inline-flex justify-center items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
                        구매자 확정
                    </button>
                `;
            }
        }

        return `
            <div class="bg-white border rounded-lg p-4 mb-4">
                <div class="flex justify-between items-center mb-2">
                    <span class="font-bold">거래 #${deal.dealId}</span>
                    <span class="px-2 py-1 text-xs rounded-full ${status.color}">${status.label}</span>
                </div>
                <p>금액: ${Number(deal.finalPrice).toLocaleString()}원</p>
                <p>생성일: ${formatDate(deal.createdAt)}</p>
                <div class="flex gap-2 mt-4">
                    <button onclick="viewDealDetail(${deal.dealId})" class="flex-1 btn-secondary">상세보기</button>
                    ${actionButtons}
                </div>
            </div>
        `;
    }

    function formatDate(dateString) {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleString('ko-KR');
    }

    function showError(message) {
        alert('오류: ' + message);
    }
    
    function showSuccess(message) {
        alert('성공: ' + message);
    }

    window.viewDealDetail = (dealId) => alert(`거래 #${dealId} 상세 보기 (구현 예정)`);
    window.openConfirmModal = (dealId) => {
        selectedDealId = dealId;
        confirmCheckbox.checked = false;
        confirmDealBtn.disabled = true;
        modal.classList.remove('hidden');
    };

    function closeConfirmModal() {
        modal.classList.add('hidden');
        selectedDealId = null;
    }

    confirmCheckbox.addEventListener('change', () => {
        confirmDealBtn.disabled = !confirmCheckbox.checked;
    });

    cancelConfirmBtn.addEventListener('click', closeConfirmModal);

    confirmDealBtn.addEventListener('click', async () => {
        if (!selectedDealId || !confirmCheckbox.checked) return;

        try {
            const response = await fetch(`/api/buyer/deals/${selectedDealId}/confirm`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || '구매자 확정에 실패했습니다.');
            }

            closeConfirmModal();
            showSuccess('구매자 확정이 완료되었습니다.');
            setTimeout(() => window.location.reload(), 1500);

        } catch (error) {
            showError(error.message);
        }
    });

    document.addEventListener('DOMContentLoaded', loadDeals);
})();
