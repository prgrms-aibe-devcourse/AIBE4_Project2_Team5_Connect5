(function () {
    let allDeals = []; // 전체 거래 데이터
    let currentTab = 'all'; // 현재 선택된 탭 (기본: 전체)
    let selectedDealId = null; // 모달에서 사용할 거래 ID

    // 모달 요소
    const modal = document.getElementById('seller-confirm-modal');
    const confirmDealBtn = document.getElementById('confirm-deal-btn');
    const cancelConfirmBtn = document.getElementById('cancel-confirm-btn');
    const confirmCheckbox = document.getElementById('seller-confirm-checkbox');

    // 탭 전환
    const tabButtons = document.querySelectorAll('.seller-deal-tab-btn');
    const tabContents = document.querySelectorAll('.seller-deal-tab-content');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.dataset.tab;
            currentTab = tabName;

            // 모든 탭 버튼 비활성화
            tabButtons.forEach(b => {
                b.classList.remove('border-gray-900', 'text-gray-900', 'font-semibold');
                b.classList.add('border-transparent', 'text-gray-500', 'font-medium');
            });

            // 클릭한 탭 버튼 활성화
            btn.classList.remove('border-transparent', 'text-gray-500', 'font-medium');
            btn.classList.add('border-gray-900', 'text-gray-900', 'font-semibold');

            // 모든 탭 콘텐츠 숨기기
            tabContents.forEach(content => {
                content.classList.add('hidden');
            });

            // 선택한 탭 콘텐츠 표시
            const targetContent = document.getElementById(`tab-${tabName}`);
            if (targetContent) {
                targetContent.classList.remove('hidden');
            }

            // 해당 탭에 맞는 데이터 표시
            displayFilteredDeals(tabName);
        });
    });

    // 데이터 로드
    async function loadDeals() {
        console.log('=== API 호출 시작 ===');

        try {
            const response = await fetch('/api/seller/deals', {
                method: 'GET',
                credentials: 'include',
                headers: {
                    'Accept': 'application/json'
                }
            });

            console.log('Response status:', response.status);

            if (!response.ok) {
                if (response.status === 401) {
                    window.location.href = '/login';
                    return;
                }
                throw new Error('API 호출 실패: ' + response.status);
            }

            const data = await response.json();
            console.log('=== 받은 데이터 ===', data);

            // 전체 데이터 저장
            allDeals = data.deals || [];

            // 탭별 카운트 업데이트
            updateTabCounts(allDeals);

            // 현재 탭에 맞는 데이터 표시
            displayFilteredDeals(currentTab);

        } catch (error) {
            console.error('=== 에러 발생 ===', error);
            showError('데이터를 불러오는데 실패했습니다: ' + error.message);
        }
    }

    // 탭별 거래 개수 업데이트
    function updateTabCounts(deals) {
        const counts = {
            all: deals.length,
            pending: 0,
            ongoing: 0,
            completed: 0,
            cancelled: 0  // 취소 + 만료
        };

        deals.forEach(deal => {
            const status = deal.status;
            if (status === 'PENDING_CONFIRMATION') {
                counts.pending++;
            } else if (status === 'CONFIRMED') {
                counts.ongoing++;
            } else if (status === 'COMPLETED') {
                counts.completed++;
            } else if (status === 'TERMINATED' || status === 'EXPIRED') {
                counts.cancelled++;  // 취소와 만료 합산
            }
        });

        // 카운트 업데이트
        document.getElementById('all-count').textContent = counts.all;
        document.getElementById('pending-count').textContent = counts.pending;
        document.getElementById('ongoing-count').textContent = counts.ongoing;
        document.getElementById('completed-count').textContent = counts.completed;
        document.getElementById('cancelled-count').textContent = counts.cancelled;
    }

    // 탭에 맞는 거래 필터링 및 표시
    function displayFilteredDeals(tabName) {
        let filteredDeals = [];
        let containerSelector = '';
        let emptyMessage = '';

        // 탭별 필터링
        switch(tabName) {
            case 'all':
                filteredDeals = allDeals; // 전체
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
                // 취소와 만료 모두 포함
                filteredDeals = allDeals.filter(d => d.status === 'TERMINATED' || d.status === 'EXPIRED');
                containerSelector = '#tab-cancelled .space-y-4';
                emptyMessage = '취소/만료된 거래가 없습니다';
                break;
        }

        const container = document.querySelector(containerSelector);
        if (!container) {
            console.error('컨테이너를 찾을 수 없습니다:', containerSelector);
            return;
        }

        // 데이터가 없는 경우
        if (filteredDeals.length === 0) {
            container.innerHTML = `
                <div class="text-center py-12 bg-gray-50 rounded-lg">
                    <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                    </svg>
                    <h3 class="mt-4 text-sm font-semibold text-gray-900">${emptyMessage}</h3>
                </div>
            `;
            return;
        }

        // 거래 카드 목록 표시
        const dealsHtml = filteredDeals.map(deal => createDealCard(deal)).join('');
        container.innerHTML = dealsHtml;
    }

    // 거래 카드 생성
    function createDealCard(deal) {
        const statusConfig = {
            'PENDING_CONFIRMATION': {
                label: '거래 대기',
                color: 'bg-yellow-100 text-yellow-800 border-yellow-300'
            },
            'CONFIRMED': {
                label: '진행 중',
                color: 'bg-blue-100 text-blue-800 border-blue-300'
            },
            'COMPLETED': {
                label: '완료',
                color: 'bg-green-100 text-green-800 border-green-300'
            },
            'TERMINATED': {
                label: '취소',
                color: 'bg-red-100 text-red-800 border-red-300'
            },
            'EXPIRED': {
                label: '만료',
                color: 'bg-gray-100 text-gray-800 border-gray-300'
            }
        };

        const status = statusConfig[deal.status] || {
            label: deal.status,
            color: 'bg-gray-100 text-gray-800 border-gray-300'
        };

        let actionButtons = '';
        if (deal.status === 'PENDING_CONFIRMATION') {
            if (deal.sellerConfirmedAt) {
                actionButtons = `<div class="flex-1 text-center text-sm font-medium text-green-600">판매 확정됨</div>`;
            } else {
                actionButtons = `
                    <button onclick="openConfirmModal(${deal.dealId})"
                            class="flex-1 inline-flex justify-center items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
                        판매자 확정
                    </button>
                `;
            }
        } else if (deal.status === 'CONFIRMED') {
             actionButtons = `<div class="flex-1 text-center text-sm font-medium text-green-600">판매 확정됨</div>`;
        }

        return `
            <div class="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-lg transition-shadow duration-200">
                <!-- 헤더 -->
                <div class="flex items-start justify-between mb-4">
                    <div class="flex-1">
                        <div class="flex items-center gap-3 mb-2">
                            <span class="text-lg font-bold text-gray-900">거래 #${deal.dealId}</span>
                            <span class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border ${status.color}">
                                ${status.label}
                            </span>
                        </div>
                    </div>
                </div>

                <!-- 내용 -->
                <div class="grid grid-cols-2 gap-4 mb-4">
                    <div>
                        <p class="text-xs text-gray-500 mb-1">거래 금액</p>
                        <p class="text-xl font-bold text-gray-900">
                            ${Number(deal.finalPrice).toLocaleString('ko-KR')}원
                        </p>
                    </div>
                    <div>
                        <p class="text-xs text-gray-500 mb-1">생성일</p>
                        <p class="text-sm font-medium text-gray-700">
                            ${formatDate(deal.createdAt)}
                        </p>
                    </div>
                </div>

                <!-- 액션 버튼 -->
                <div class="flex gap-2 pt-4 border-t border-gray-100">
                    <button onclick="viewDealDetail(${deal.dealId})" 
                            class="flex-1 inline-flex justify-center items-center px-4 py-2 border border-gray-300 shadow-sm text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
                        상세보기
                    </button>
                    ${actionButtons}
                </div>
            </div>
        `;
    }

    // 날짜 포맷팅
    function formatDate(dateString) {
        if (!dateString) return 'N/A';
        try {
            const date = new Date(dateString);
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');
            return `${year}-${month}-${day} ${hours}:${minutes}`;
        } catch (e) {
            return dateString;
        }
    }

    // 에러 메시지 표시
    function showError(message) {
        const errorDiv = document.createElement('div');
        errorDiv.className = 'fixed top-4 right-4 bg-red-500 text-white px-6 py-3 rounded-lg shadow-lg z-50';
        errorDiv.textContent = message;
        document.body.appendChild(errorDiv);

        setTimeout(() => {
            errorDiv.remove();
        }, 5000);
    }

    // 전역 함수: 거래 상세보기
    window.viewDealDetail = function(dealId) {
        window.location.href = `/seller/deals/${dealId}`;
    };

    // 전역 함수: 모달 열기
    window.openConfirmModal = function(dealId) {
        selectedDealId = dealId;
        confirmCheckbox.checked = false;
        confirmDealBtn.disabled = true;
        modal.classList.remove('hidden');
    };

    // 모달 닫기
    function closeConfirmModal() {
        modal.classList.add('hidden');
        selectedDealId = null;
    }

    // 체크박스 상태에 따라 확인 버튼 활성화/비활성화
    confirmCheckbox.addEventListener('change', () => {
        confirmDealBtn.disabled = !confirmCheckbox.checked;
    });

    // 모달 취소 버튼
    cancelConfirmBtn.addEventListener('click', closeConfirmModal);

    // 모달 확인 버튼 (API 호출)
    confirmDealBtn.addEventListener('click', async () => {
        if (!selectedDealId || !confirmCheckbox.checked) {
            return;
        }

        try {
            const response = await fetch(`/api/seller/deals/${selectedDealId}/confirm`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || '판매자 확정에 실패했습니다.');
            }

            // 성공 시
            closeConfirmModal();
            showSuccess('판매자 확정이 완료되었습니다.');
            
            // 1.5초 후 페이지 새로고침
            setTimeout(() => {
                window.location.reload();
            }, 1500);

        } catch (error) {
            console.error('판매자 확정 API 호출 실패:', error);
            showError(error.message);
        }
    });

    // 성공 메시지 표시
    function showSuccess(message) {
        const successDiv = document.createElement('div');
        successDiv.className = 'fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg z-50';
        successDiv.textContent = message;
        document.body.appendChild(successDiv);

        setTimeout(() => {
            successDiv.remove();
        }, 3000);
    }

    // 페이지 로드 시 데이터 로드
    console.log('=== seller-deals.js 로드됨 ===');
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadDeals);
    } else {
        loadDeals();
    }
})();
