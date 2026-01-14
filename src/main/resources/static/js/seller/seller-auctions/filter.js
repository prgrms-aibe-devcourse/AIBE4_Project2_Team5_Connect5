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

    // ===== Sorting helpers (DRAFT 항상 맨 뒤) =====
    const normalizeStatus = (it) => String(it?.status ?? '').toUpperCase();
    const isDraft = (it) => normalizeStatus(it) === 'DRAFT';

    const toNumber = (v) => {
        const n = Number(v);
        return Number.isFinite(n) ? n : 0;
    };

    const toTime = (v) => {
        if (!v) return 0;
        const t = new Date(v).getTime();
        return Number.isFinite(t) ? t : 0;
    };

    // 최신순 기준: updatedAt > createdAt > endAt
    const latestBase = (it) =>
        toTime(it?.updatedAt) ||
        toTime(it?.createdAt) ||
        toTime(it?.endAt) ||
        0;

    // 마감임박: endAt 오름차순(없으면 맨 뒤)
    const deadlineBase = (it) => {
        const t = toTime(it?.endAt);
        return t > 0 ? t : Number.MAX_SAFE_INTEGER;
    };

    // 가격: ENDED_SOLD면 낙찰가 계열, 아니면 현재가/최고가/시작가
    const priceBase = (it) => {
        const st = normalizeStatus(it);
        if (st === 'ENDED_SOLD') {
            return (
                toNumber(it?.finalPrice) ||
                toNumber(it?.closedPrice) ||
                toNumber(it?.winningBidAmount) ||
                toNumber(it?.soldPrice) ||
                0
            );
        }
        return (
            toNumber(it?.currentPrice) ||
            toNumber(it?.highestBidAmount) ||
            toNumber(it?.highestBid) ||
            toNumber(it?.startPrice) ||
            0
        );
    };

    // 인기: favorite/like/view/bidCount fallback
    const popularBase = (it) =>
        toNumber(it?.favoriteCount) ||
        toNumber(it?.likeCount) ||
        toNumber(it?.viewCount) ||
        toNumber(it?.bidCount) ||
        0;

    const buildComparator = (sortKey) => {
        const key = String(sortKey || 'latest');

        let cmp2;
        if (key === 'deadline') cmp2 = (a, b) => deadlineBase(a) - deadlineBase(b);
        else if (key === 'price_asc') cmp2 = (a, b) => priceBase(a) - priceBase(b);
        else if (key === 'price_desc') cmp2 = (a, b) => priceBase(b) - priceBase(a);
        else if (key === 'popular') cmp2 = (a, b) => popularBase(b) - popularBase(a);
        else cmp2 = (a, b) => latestBase(b) - latestBase(a); // latest

        return (a, b) => {
            // 1) DRAFT는 항상 뒤로
            const r1 = (isDraft(a) ? 1 : 0) - (isDraft(b) ? 1 : 0);
            if (r1 !== 0) return r1;

            // 2) 사용자가 선택한 정렬
            const r2 = cmp2(a, b);
            if (r2 !== 0) return r2;

            // 3) tie-breaker
            const aid = String(a?.auctionId ?? a?.id ?? '');
            const bid = String(b?.auctionId ?? b?.id ?? '');
            return aid.localeCompare(bid);
        };
    };

    const sortWithDraftLast = (items, sortKey) => {
        if (!Array.isArray(items) || items.length === 0) return [];
        return [...items].sort(buildComparator(sortKey));
    };

    // ===== 전체 탭에서만 "전체를 다 가져와서" DRAFT를 진짜 맨 뒤로 보내기 =====
    const FULL_FETCH_PAGE_SIZE = 200;
    const FULL_FETCH_MAX_PAGES = 30; // 200 * 30 = 6000개 상한 (판매자 본인 목록이면 충분)
    const fetchAllAuctionsForSeller = async ({ sortKey }) => {
        const all = [];
        for (let p = 0; p < FULL_FETCH_MAX_PAGES; p++) {
            const data = await api.getAuctions({
                sellerId,
                page: p,
                size: FULL_FETCH_PAGE_SIZE,
                sort: 'latest', // 서버 정렬은 의미 없음(어차피 클라에서 재정렬)
                status: null,
            });

            const chunk = Array.isArray(data?.content) ? data.content : [];
            if (chunk.length === 0) break;

            all.push(...chunk);

            const pageInfo = data?.pageInfo ?? {};
            const totalPages = Number(pageInfo?.totalPages ?? 0);
            const hasNext = Boolean(pageInfo?.hasNext);

            // pageInfo가 신뢰 가능하면 조기 종료
            if (totalPages && p >= totalPages - 1) break;
            if (pageInfo && 'hasNext' in pageInfo && !hasNext) break;
        }

        return sortWithDraftLast(all, sortKey);
    };

    // ===== Loaders =====
    const loadCountsAndSummary = async () => {
        const [all, live, closed, draft] = await Promise.all([
            api.fetchCount({ sellerId, status: null }),
            api.fetchCount({ sellerId, status: 'LIVE' }),
            api.fetchCount({ sellerId, status: 'ENDED_SOLD' }),
            api.fetchCount({ sellerId, status: 'DRAFT' }),
        ]);

        $tabAllCount.textContent = String(all);
        $tabLiveCount.textContent = String(live);
        $tabClosedCount.textContent = String(closed);
        $tabDraftCount.textContent = String(draft);

        $sumLive.textContent = String(live);
        $sumClosed.textContent = String(closed);
        $sumDraft.textContent = String(draft);

        const revenue = await api.fetchRevenue({ sellerId }).catch(() => 0);
        $sumRevenue.textContent = list.formatWon(revenue);
    };

    const renderClientPaged = ({ items }) => {
        const total = items.length;

        if (total === 0) {
            list.renderRows({ tbodyEl: $tbody, items: [] });
            showEmpty(state.activeTab);
            list.updateRangeText({ rangeTextEl: $rangeText, total: 0, start: 0, end: 0 });
            $paginationNav.innerHTML = '';
            return;
        }

        const totalPages = Math.ceil(total / state.size);
        const currentPage = Math.min(state.page, totalPages - 1);

        const slice = items.slice(currentPage * state.size, currentPage * state.size + state.size);

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
    };

    const loadList = async () => {
        const status = tabToStatus[state.activeTab];

        try {
            // =========================
            // 1) "전체 탭(all)" + "검색어 없음" => 전체 fetch 후 클라 정렬/페이지
            //    (여기서 DRAFT를 진짜 전체 기준 맨 뒤로 보장)
            // =========================
            const isAllTab = state.activeTab === 'all';
            const hasSearch = state.search && state.search.trim().length > 0;

            if (isAllTab && !hasSearch) {
                const allSorted = await fetchAllAuctionsForSeller({ sortKey: state.sort });
                renderClientPaged({ items: allSorted });
                return;
            }

            // =========================
            // 2) 검색어 있으면: 크게 받아서 클라 필터 + 클라 페이지
            // =========================
            if (hasSearch) {
                const data = await api.getAuctions({
                    sellerId,
                    page: 0,
                    size: 500, // 검색은 더 넉넉하게
                    sort: state.sort,
                    status,
                });

                const raw = Array.isArray(data?.content) ? data.content : [];
                const q = state.search.trim().toLowerCase();

                const filtered = raw.filter((it) => String(it?.title ?? '').toLowerCase().includes(q));

                // ✅ 검색에서도 DRAFT는 뒤로
                const sorted = sortWithDraftLast(filtered, state.sort);

                renderClientPaged({ items: sorted });
                return;
            }

            // =========================
            // 3) 그 외(live/closed/draft 탭) => 서버 페이지네이션
            //    (draft 탭은 애초에 DRAFT만 오므로 문제 없음)
            // =========================
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

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
