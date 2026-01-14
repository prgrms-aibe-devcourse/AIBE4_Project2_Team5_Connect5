// src/main/resources/static/js/buyer/buyer-deals.js

document.addEventListener('DOMContentLoaded', () => {
    initBuyerDealTabs();
    loadBuyerDeals();
});

// 탭 전환
function initBuyerDealTabs() {
    const tabButtons = document.querySelectorAll('.buyer-deal-tab-btn');
    const tabContents = document.querySelectorAll('.buyer-deal-tab-content');

    tabButtons.forEach((btn) => {
        btn.addEventListener('click', () => {
            const target = btn.getAttribute('data-tab');

            tabButtons.forEach((b) => {
                b.classList.remove('border-gray-900', 'text-gray-900', 'font-semibold');
                b.classList.add('border-transparent', 'text-gray-500');
            });
            btn.classList.remove('border-transparent', 'text-gray-500');
            btn.classList.add('border-gray-900', 'text-gray-900', 'font-semibold');

            tabContents.forEach((content) => {
                content.classList.add('hidden');
            });

            const activeTab = document.getElementById(`buyer-tab-${target}`);
            if (activeTab) {
                activeTab.classList.remove('hidden');
            }
        });
    });
}

// 거래 목록 불러오기
async function loadBuyerDeals() {
    try {
        const res = await fetch('/api/buyer/deals');
        if (!res.ok) {
            throw new Error('구매자 거래 목록을 불러오지 못했습니다.');
        }

        const data = await res.json(); // { buyerId, totalCount, deals: [...] }
        const deals = data.deals ?? [];

        renderBuyerDealLists(deals);
    } catch (error) {
        console.error(error);
        alert('구매자 거래 목록 조회 중 오류가 발생했습니다.');
    }
}

// 상태 → 탭 이름 매핑
function mapBuyerStatusToTab(status) {
    switch (status) {
        case 'PENDING_CONFIRMATION':
            return 'pending';
        case 'CONFIRMED':
            return 'ongoing';
        case 'COMPLETED':
            return 'completed';
        case 'TERMINATED':
        case 'EXPIRED':
            return 'cancelled';
        default:
            return 'all';
    }
}

// 상태 배지 스타일
function getBuyerStatusBadgeInfo(status) {
    switch (status) {
        case 'PENDING_CONFIRMATION':
            return {
                badgeClass: 'bg-yellow-100 text-yellow-800',
                badgeLabel: '거래 대기',
            };
        case 'CONFIRMED':
            return {
                badgeClass: 'bg-blue-100 text-blue-800',
                badgeLabel: '진행 중',
            };
        case 'COMPLETED':
            return {
                badgeClass: 'bg-green-100 text-green-800',
                badgeLabel: '완료',
            };
        case 'TERMINATED':
            return {
                badgeClass: 'bg-red-100 text-red-800',
                badgeLabel: '취소',
            };
        case 'EXPIRED':
            return {
                badgeClass: 'bg-gray-100 text-gray-800',
                badgeLabel: '만료',
            };
        default:
            return {
                badgeClass: 'bg-gray-100 text-gray-800',
                badgeLabel: status || '알수없음',
            };
    }
}

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// 🔹 판매 목록과 거의 같은 레이아웃 + 완료 상태에서만 "리뷰 작성" 버튼 노출
function createBuyerDealCard(deal) {
    const title = deal.auctionTitle || '제목 없는 경매';
    const price = deal.finalPrice != null
        ? new Intl.NumberFormat('ko-KR').format(deal.finalPrice)
        : '-';
    const createdAt = deal.createdAt
        ? deal.createdAt.replace('T', ' ').slice(0, 16)
        : '';
    const imageUrl = deal.thumbnailUrl || '/images/placeholder.png';

    const { badgeClass, badgeLabel } = getBuyerStatusBadgeInfo(deal.status);

    const detailUrl = `/buyer/deals/${deal.dealId}`;
    // ✅ 리뷰 작성 페이지 URL (컨트롤러 매핑과 맞춰서 사용)
    const reviewUrl = `/buyer/deals/${deal.dealId}/review`;
    const isCompleted = deal.status === 'COMPLETED';

    return `
    <article class="group bg-white rounded-xl shadow-sm hover:shadow-md border border-gray-100 overflow-hidden transition">
      <div class="flex gap-4">
        <!-- 썸네일 -->
        <div class="relative w-32 h-24 flex-shrink-0 bg-gray-100 overflow-hidden">
          <img src="${imageUrl}"
               alt="${escapeHtml(title)}"
               class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
               onerror="this.src='/images/placeholder.png'">
        </div>

        <!-- 본문 -->
        <div class="flex-1 py-4 pr-4 flex flex-col justify-between">
          <!-- 상단: 제목 + 상태 뱃지 -->
          <div class="flex items-start justify-between gap-4">
            <div>
              <h3 class="text-sm font-semibold text-gray-900 line-clamp-2">
                ${escapeHtml(title)}
              </h3>
              <p class="mt-1 text-xs text-gray-500">${createdAt}</p>
            </div>
            <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${badgeClass}">
              ${badgeLabel}
            </span>
          </div>

          <!-- 하단: 거래 금액 + 버튼들 -->
          <div class="mt-3 flex items-center justify-between">
            <div class="text-sm font-bold text-gray-900">
              ${price}<span class="ml-0.5 text-xs text-gray-500">원</span>
            </div>
            <div class="flex items-center gap-2">
              <button
                type="button"
                class="inline-flex items-center px-4 py-1.5 border border-gray-300 rounded-full text-xs font-medium text-gray-700 hover:bg-gray-50"
                onclick="window.location.href='${detailUrl}'">
                상세보기
              </button>
              ${isCompleted ? `
              <button
                type="button"
                class="inline-flex items-center px-4 py-1.5 rounded-full text-xs font-medium text-white bg-indigo-600 hover:bg-indigo-700"
                onclick="window.location.href='${reviewUrl}'">
                리뷰 작성
              </button>
              ` : ''}
            </div>
          </div>
        </div>
      </div>
    </article>
  `;
}

// 목록 렌더링
function renderBuyerDealLists(deals) {
    const containers = {
        all: document.getElementById('buyer-deal-list-all'),
        pending: document.getElementById('buyer-deal-list-pending'),
        ongoing: document.getElementById('buyer-deal-list-ongoing'),
        completed: document.getElementById('buyer-deal-list-completed'),
        cancelled: document.getElementById('buyer-deal-list-cancelled'),
    };

    const empties = {
        all: document.getElementById('buyer-deal-empty-all'),
        pending: document.getElementById('buyer-deal-empty-pending'),
        ongoing: document.getElementById('buyer-deal-empty-ongoing'),
        completed: document.getElementById('buyer-deal-empty-completed'),
        cancelled: document.getElementById('buyer-deal-empty-cancelled'),
    };

    const counts = {
        all: document.getElementById('buyer-all-count'),
        pending: document.getElementById('buyer-pending-count'),
        ongoing: document.getElementById('buyer-ongoing-count'),
        completed: document.getElementById('buyer-completed-count'),
        cancelled: document.getElementById('buyer-cancelled-count'),
    };

    // 초기화
    Object.values(containers).forEach((el) => {
        if (el) el.innerHTML = '';
    });

    const countMap = {
        all: deals.length,
        pending: 0,
        ongoing: 0,
        completed: 0,
        cancelled: 0,
    };

    deals.forEach((deal) => {
        const cardHtml = createBuyerDealCard(deal);

        // 전체 탭
        if (containers.all) {
            containers.all.insertAdjacentHTML('beforeend', cardHtml);
        }

        // 상태별 탭
        const tab = mapBuyerStatusToTab(deal.status);
        if (containers[tab]) {
            containers[tab].insertAdjacentHTML('beforeend', cardHtml);
            countMap[tab] += 1;
        }
    });

    // 개수 표시
    Object.entries(counts).forEach(([key, el]) => {
        if (el && typeof countMap[key] === 'number') {
            el.textContent = String(countMap[key]);
        }
    });

    // 비어 있는 탭 처리
    Object.entries(containers).forEach(([key, listEl]) => {
        const emptyEl = empties[key];
        if (!listEl || !emptyEl) return;

        if (listEl.children.length === 0) {
            emptyEl.classList.remove('hidden');
        } else {
            emptyEl.classList.add('hidden');
        }
    });
}
