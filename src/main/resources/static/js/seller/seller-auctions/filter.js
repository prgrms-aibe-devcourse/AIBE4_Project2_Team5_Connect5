// /static/js/seller/seller-auctions.filter.js
(() => {
    'use strict';

    const $page = document.getElementById('seller-auctions-page');
    if (!$page) return;

    const ns = (window.SellerAuctions = window.SellerAuctions || {});
    const api = ns.api;
    const list = ns.list;

    if (!api || !list) {
        console.error('seller-auctions.api.js / seller-auctions.list.js 로드 순서를 확인해주세요.');
        return;
    }

    const sellerId = $page.dataset.sellerId;

    // ===== DOM =====
    const $tbody = document.getElementById('sa-tbody');
    const $empty = document.getElementById('sa-empty');
    const $emptyTitle = document.getElementById('sa-empty-title');
    const $emptyDesc = document.getElementById('sa-empty-desc');

    const $sort = document.getElementById('sa-sort');
    const $search = document.getElementById('sa-search-input');

    const $rangeText = document.getElementById('sa-range-text');
    const $paginationNav = document.getElementById('sa-pagination');

    // Summary
    const $sumLive = document.getElementById('summary-live-count');
    const $sumClosed = document.getElementById('summary-closed-count');
    const $sumRevenue = document.getElementById('summary-revenue-amount');
    const $sumDraft = document.getElementById('summary-draft-count');

    // Tab counts
    const $tabAllCount = document.getElementById('tab-all-count');
    const $tabLiveCount = document.getElementById('live-count');
    const $tabClosedCount = document.getElementById('closed-count');
    const $tabDraftCount = document.getElementById('draft-count');

    const tabToStatus = {
        all: null,
        live: 'LIVE',
        closed: 'ENDED_SOLD',
        draft: 'DRAFT',
    };

    const tabEmptyText = {
        all: ['목록이 비어 있습니다.', '등록한 경매가 없습니다.'],
        live: ['진행 중인 경매가 없습니다.', '진행 중인 경매가 없습니다.'],
        closed: ['낙찰 완료된 경매가 없습니다.', '낙찰 완료된 경매가 없습니다.'],
        draft: ['임시 저장된 경매가 없습니다.', '작성 중인 경매는 자동으로 저장됩니다.'],
    };

    // ===== State =====
    const state = {
        activeTab: 'all',
        page: 0,
        size: 8,
        sort: 'latest',
        search: '',
    };

    // ===== UI helpers =====
    const setActiveTabUi = (tab) => {
        document.querySelectorAll('.sa-tab').forEach((btn) => {
            const isActive = btn.dataset.tab === tab;
            btn.classList.toggle('is-active', isActive);

            btn.classList.toggle('text-slate-900', isActive);
            btn.classList.toggle('border-slate-900', isActive);

            btn.classList.toggle('text-slate-500', !isActive);
            btn.classList.toggle('border-transparent', !isActive);
        });
    };

    const showEmpty = (tab) => {
        const [t, d] = tabEmptyText[tab] ?? tabEmptyText.all;
        $emptyTitle.textContent = t;
        $emptyDesc.textContent = d;
        $empty.classList.remove('hidden');
    };

    const hideEmpty = () => {
        $empty.classList.add('hidden');
    };

    // ===== Loaders =====
    const loadCountsAndSummary = async () => {
        const [all, live, closed, draft] = await Promise.all([
            api.fetchCount({ sellerId, status: null }),
            api.fetchCount({ sellerId, status: 'LIVE' }),
            api.fetchCount({ sellerId, status: 'ENDED_SOLD' }),
            api.fetchCount({ sellerId, status: 'DRAFT' }),
        ]);

        // 탭 카운트
        $tabAllCount.textContent = String(all);
        $tabLiveCount.textContent = String(live);
        $tabClosedCount.textContent = String(closed);
        $tabDraftCount.textContent = String(draft);

        // 요약 카드
        $sumLive.textContent = String(live);
        $sumClosed.textContent = String(closed);
        $sumDraft.textContent = String(draft);

        // 총 수익
        const revenue = await api.fetchRevenue({ sellerId }).catch(() => 0);
        $sumRevenue.textContent = list.formatWon(revenue);
    };

    const loadList = async () => {
        const status = tabToStatus[state.activeTab];

        try {
            // 검색어가 있으면: 0페이지에서 크게 받아서 클라에서 필터 + 클라 페이지네이션
            if (state.search && state.search.trim().length > 0) {
                const data = await api.getAuctions({
                    sellerId,
                    page: 0,
                    size: 200,
                    sort: state.sort,
                    status,
                });

                const raw = Array.isArray(data?.content) ? data.content : [];
                const q = state.search.trim().toLowerCase();
                const filtered = raw.filter((it) => String(it?.title ?? '').toLowerCase().includes(q));

                const total = filtered.length;
                if (total === 0) {
                    list.renderRows({ tbodyEl: $tbody, items: [] });
                    showEmpty(state.activeTab);
                    list.updateRangeText({ rangeTextEl: $rangeText, total: 0, start: 0, end: 0 });
                    $paginationNav.innerHTML = '';
                    return;
                }

                const totalPages = Math.ceil(total / state.size);
                const currentPage = Math.min(state.page, totalPages - 1);

                const slice = filtered.slice(
                    currentPage * state.size,
                    currentPage * state.size + state.size
                );

                hideEmpty();
                list.renderRows({ tbodyEl: $tbody, items: slice });

                const start = currentPage * state.size + 1;
                const end = currentPage * state.size + slice.length;
                list.updateRangeText({ rangeTextEl: $rangeText, total, start, end });

                list.renderPagination(
                    {
                        paginationEl: $paginationNav,
                        pageInfo: {
                            currentPage,
                            totalPages,
                            hasPrevious: currentPage > 0,
                            hasNext: currentPage < totalPages - 1,
                        },
                    },
                    (p) => {
                        state.page = p;
                        loadList();
                    }
                );

                return;
            }

            // 검색어 없으면: 서버 페이지네이션 그대로 사용
            const data = await api.getAuctions({
                sellerId,
                page: state.page,
                size: state.size,
                sort: state.sort,
                status,
            });

            const items = Array.isArray(data?.content) ? data.content : [];
            const pageInfo = data?.pageInfo ?? {};
            const total = Number(pageInfo?.totalElements ?? 0);

            if (items.length === 0) {
                list.renderRows({ tbodyEl: $tbody, items: [] });
                showEmpty(state.activeTab);
                list.updateRangeText({ rangeTextEl: $rangeText, total: 0, start: 0, end: 0 });
                $paginationNav.innerHTML = '';
                return;
            }

            hideEmpty();
            list.renderRows({ tbodyEl: $tbody, items });

            const currentPage = Number(pageInfo?.currentPage ?? 0);
            const totalPages = Number(pageInfo?.totalPages ?? 1);

            const start = currentPage * state.size + 1;
            const end = currentPage * state.size + items.length;
            list.updateRangeText({ rangeTextEl: $rangeText, total, start, end });

            list.renderPagination(
                {
                    paginationEl: $paginationNav,
                    pageInfo: {
                        currentPage,
                        totalPages,
                        hasPrevious: Boolean(pageInfo?.hasPrevious),
                        hasNext: Boolean(pageInfo?.hasNext),
                    },
                },
                (p) => {
                    state.page = p;
                    loadList();
                }
            );
        } catch (e) {
            console.error(e);
            list.renderRows({ tbodyEl: $tbody, items: [] });
            $emptyTitle.textContent = '목록을 불러오지 못했습니다.';
            $emptyDesc.textContent = '잠시 후 다시 시도해주세요.';
            $empty.classList.remove('hidden');
            list.updateRangeText({ rangeTextEl: $rangeText, total: 0, start: 0, end: 0 });
            $paginationNav.innerHTML = '';
        }
    };

    // ===== Events =====
    const bindEvents = () => {
        // tabs
        document.querySelectorAll('.sa-tab').forEach((btn) => {
            btn.addEventListener('click', () => {
                const tab = btn.dataset.tab;
                if (!tab || tab === state.activeTab) return;

                state.activeTab = tab;
                state.page = 0;
                state.search = '';
                if ($search) $search.value = '';

                setActiveTabUi(tab);
                loadList();
            });
        });

        // sort
        $sort?.addEventListener('change', () => {
            state.sort = $sort.value || 'latest';
            state.page = 0;
            loadList();
        });

        // search (debounce)
        let t = null;
        $search?.addEventListener('input', () => {
            if (t) clearTimeout(t);
            t = setTimeout(() => {
                state.search = ($search.value || '').trim();
                state.page = 0;
                loadList();
            }, 250);
        });
    };

    // ===== Init =====
    const init = async () => {
        setActiveTabUi(state.activeTab);
        bindEvents();

        list.bindRowInteractions({
            tbodyEl: $tbody,
            onRelist: async (auctionId) => {
                const draftAuctionId = await api.relist(auctionId);
                window.location.href = `/seller/auctions/drafts/${encodeURIComponent(draftAuctionId)}`;
            },
        });

        await loadCountsAndSummary().catch((e) => console.error(e));
        await loadList();
    };

    // defer 로드라 DOMContentLoaded 없이도 대부분 안전하지만, 방어적으로 유지
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
