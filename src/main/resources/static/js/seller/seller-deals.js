(function () {
    // 탭 전환
    const tabButtons = document.querySelectorAll('.seller-deal-tab-btn');
    const tabContents = document.querySelectorAll('.seller-deal-tab-content');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.dataset.tab;

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
        });
    });

    // 데이터 로드
    async function loadDeals() {
        try {
            const response = await fetch('/api/seller/deals', {
                method: 'GET',
                credentials: 'include',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                if (response.status === 401) {
                    window.location.href = '/login';
                    return;
                }
                throw new Error('거래 정보를 불러오는데 실패했습니다.');
            }

            const data = await response.json();

            updateCounts(
                data.counts.pending,
                data.counts.ongoing,
                data.counts.completed,
                data.counts.cancelled
            );

            renderDeals('pending', data.pending);
            renderDeals('ongoing', data.ongoing);
            renderDeals('completed', data.completed);
            renderDeals('cancelled', data.cancelled);

        } catch (error) {
            console.error('거래 정보 로드 오류:', error);
            showError('거래 정보를 불러오는데 실패했습니다.');
        }
    }

    function updateCounts(pending, ongoing, completed, cancelled) {
        const pendingCount = document.getElementById('pending-count');
        const ongoingCount = document.getElementById('ongoing-count');
        const completedCount = document.getElementById('completed-count');
        const cancelledCount = document.getElementById('cancelled-count');

        if (pendingCount) pendingCount.textContent = pending;
        if (ongoingCount) ongoingCount.textContent = ongoing;
        if (completedCount) completedCount.textContent = completed;
        if (cancelledCount) cancelledCount.textContent = cancelled;
    }

    function renderDeals(tabName, deals) {
        const container = document.getElementById(`${tabName}-list`);
        const emptyState = document.querySelector(`#tab-${tabName} > div > div:first-child`);

        if (!container) return;

        if (!deals || deals.length === 0) {
            container.classList.add('hidden');
            if (emptyState) emptyState.classList.remove('hidden');
        } else {
            container.classList.remove('hidden');
            if (emptyState) emptyState.classList.add('hidden');
            container.innerHTML = deals.map(deal => createDealCard(deal, tabName)).join('');
        }
    }

    function createDealCard(deal, status) {
        const statusText = getStatusText(deal.status);
        const statusColor = getStatusColor(deal.status);
        const formattedPrice = Number(deal.finalPrice).toLocaleString('ko-KR');
        const createdDate = new Date(deal.createdAt).toLocaleDateString('ko-KR');

        return `
            <div class="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow">
                <div class="flex justify-between items-start mb-4">
                    <div class="flex-1">
                        <h3 class="text-lg font-semibold text-gray-900 mb-2">
                            ${escapeHtml(deal.auctionTitle)}
                        </h3>
                        <div class="text-sm text-gray-600 space-y-1">
                            <p>구매자: ${escapeHtml(deal.buyerName)}</p>
                            <p>거래 금액: <span class="font-semibold">${formattedPrice}원</span></p>
                            <p>생성일: ${createdDate}</p>
                        </div>
                    </div>
                    <div>
                        <span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${statusColor}">
                            ${statusText}
                        </span>
                    </div>
                </div>
                ${renderDealActions(deal, status)}
            </div>
        `;
    }

    function renderDealActions(deal, status) {
        if (status === 'pending') {
            const confirmDisabled = deal.sellerConfirmed;
            const confirmClass = confirmDisabled ? 'opacity-50 cursor-not-allowed bg-gray-400' : 'bg-blue-600 hover:bg-blue-700';

            return `
                <div class="flex gap-2 mt-4">
                    <button 
                        class="px-4 py-2 text-white rounded-lg transition-colors ${confirmClass}"
                        onclick="confirmDeal(${deal.dealId})"
                        ${confirmDisabled ? 'disabled' : ''}
                    >
                        ${confirmDisabled ? '✓ 확정 완료' : '거래 확정'}
                    </button>
                    <a href="/seller/deals/${deal.dealId}" 
                       class="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 transition-colors">
                        상세보기
                    </a>
                </div>
            `;
        } else {
            return `
                <div class="flex gap-2 mt-4">
                    <a href="/seller/deals/${deal.dealId}" 
                       class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                        상세보기
                    </a>
                </div>
            `;
        }
    }

    function getStatusText(status) {
        const statusMap = {
            'PENDING_CONFIRMATION': '확정 대기',
            'CONFIRMED': '진행 중',
            'COMPLETED': '완료',
            'TERMINATED': '취소',
            'EXPIRED': '만료'
        };
        return statusMap[status] || status;
    }

    function getStatusColor(status) {
        const colorMap = {
            'PENDING_CONFIRMATION': 'bg-yellow-100 text-yellow-800',
            'CONFIRMED': 'bg-blue-100 text-blue-800',
            'COMPLETED': 'bg-green-100 text-green-800',
            'TERMINATED': 'bg-red-100 text-red-800',
            'EXPIRED': 'bg-gray-100 text-gray-800'
        };
        return colorMap[status] || 'bg-gray-100 text-gray-800';
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function showError(message) {
        const errorDiv = document.createElement('div');
        errorDiv.className = 'fixed top-4 right-4 bg-red-500 text-white px-6 py-3 rounded-lg shadow-lg z-50';
        errorDiv.textContent = message;
        document.body.appendChild(errorDiv);
        setTimeout(() => errorDiv.remove(), 3000);
    }

    function showSuccess(message) {
        const successDiv = document.createElement('div');
        successDiv.className = 'fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg z-50';
        successDiv.textContent = message;
        document.body.appendChild(successDiv);
        setTimeout(() => successDiv.remove(), 3000);
    }

    window.confirmDeal = async function(dealId) {
        if (!confirm('거래를 확정하시겠습니까?\n확정 후에는 취소할 수 없습니다.')) {
            return;
        }

        try {
            const response = await fetch(`/api/seller/deals/${dealId}/confirm`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(response.status === 403 ? '권한이 없습니다.' : '거래 확정에 실패했습니다.');
            }

            showSuccess('거래가 확정되었습니다.');
            setTimeout(() => loadDeals(), 1000);

        } catch (error) {
            console.error('거래 확정 오류:', error);
            showError(error.message);
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadDeals);
    } else {
        loadDeals();
    }
})();
