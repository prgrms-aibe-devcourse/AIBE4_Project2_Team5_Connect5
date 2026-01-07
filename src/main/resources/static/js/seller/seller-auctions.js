// /static/js/seller/seller-auctions.js
(() => {
    'use strict';

    const $page = document.getElementById('seller-auctions-page');
    if (!$page) return;

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
    const $tabEndedCount = document.getElementById('ended-count');
    const $tabDraftCount = document.getElementById('draft-count');

    const tabToStatus = {
        all: null,
        live: 'LIVE',
        closed: 'ENDED_SOLD',
        ended: 'ENDED_UNSOLD',
        draft: 'DRAFT',
    };

    const tabEmptyText = {
        all: ['목록이 비어 있습니다.', '등록한 경매가 없습니다.'],
        live: ['진행 중인 경매가 없습니다.', '진행 중인 경매가 없습니다.'],
        closed: ['낙찰 완료된 경매가 없습니다.', '낙찰 완료된 경매가 없습니다.'],
        ended: ['유찰된 경매가 없습니다.', '유찰된 경매가 없습니다.'],
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

    // ===== Utils =====
    const escapeHtml = (s) =>
        String(s ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');

    const formatWon = (n) => {
        const v = Number(n ?? 0);
        if (!Number.isFinite(v)) return '0원';
        return v.toLocaleString('ko-KR') + '원';
    };

    // yyyy년 mm월 dd일 HH:mm (KST)
    const formatKstYmdHm = (iso) => {
        if (!iso) return '-';
        const d = new Date(iso);
        if (Number.isNaN(d.getTime())) return '-';

        const parts = new Intl.DateTimeFormat('ko-KR', {
            timeZone: 'Asia/Seoul',
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            hour12: false,
        }).formatToParts(d);

        const get = (type) => parts.find((p) => p.type === type)?.value ?? '';
        const year = get('year');
        const month = get('month');
        const day = get('day');
        const hour = get('hour');
        const minute = get('minute');

        return `${year}년 ${month}월 ${day}일 ${hour}:${minute}`;
    };

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

    const getCsrf = () => {
        // (선택) Spring Security CSRF가 켜져 있으면 base.html에 아래 meta가 있을 수 있습니다.
        // <meta name="_csrf" content="...">
        // <meta name="_csrf_header" content="X-CSRF-TOKEN">
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        if (!token || !header) return null;
        return { header, token };
    };

    const apiGetAuctions = async ({ page, size, sort, status }) => {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(size));
        params.set('sort', sort || 'latest');
        params.set('sellerId', sellerId);
        if (status) params.set('status', status);

        const res = await fetch(`/api/auctions?${params.toString()}`, {
            method: 'GET',
            credentials: 'same-origin',
            headers: { Accept: 'application/json' },
        });

        if (!res.ok) {
            const text = await res.text().catch(() => '');
            throw new Error(`API ${res.status} ${res.statusText} ${text}`);
        }
        return res.json();
    };

    // 유찰 재등록 (서버에서 DRAFT 복제 생성 후 draftAuctionId 반환)
    const apiRelist = async (auctionId) => {
        const csrf = getCsrf();
        const headers = { Accept: 'application/json' };
        if (csrf) headers[csrf.header] = csrf.token;

        const res = await fetch(`/api/seller/auctions/${encodeURIComponent(auctionId)}/relist`, {
            method: 'POST',
            credentials: 'same-origin',
            headers,
        });

        if (!res.ok) {
            const text = await res.text().catch(() => '');
            throw new Error(`RELIS_API ${res.status} ${res.statusText} ${text}`);
        }

        const data = await res.json().catch(() => ({}));
        const draftAuctionId = data?.draftAuctionId;
        if (!draftAuctionId) throw new Error('재등록 응답에 draftAuctionId가 없습니다.');
        return draftAuctionId;
    };

    const fetchCount = async (status) => {
        const data = await apiGetAuctions({ page: 0, size: 1, sort: 'latest', status });
        return Number(data?.pageInfo?.totalElements ?? 0);
    };

    const fetchRevenue = async () => {
        let page = 0;
        const size = 100;
        let sum = 0;

        while (true) {
            const data = await apiGetAuctions({ page, size, sort: 'latest', status: 'ENDED_SOLD' });
            const items = Array.isArray(data?.content) ? data.content : [];

            for (const it of items) {
                const v = Number(it?.finalPrice ?? 0);
                if (Number.isFinite(v)) sum += v;
            }

            if (!data?.pageInfo?.hasNext) break;
            page += 1;
            if (page > 50) break; // 안전장치
        }

        return sum;
    };

    // ===== Icons (SVG) =====
    const svgHeart = () => `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
         fill="none" stroke="currentColor" stroke-width="1.8"
         class="h-4 w-4 text-slate-500">
      <path stroke-linecap="round" stroke-linejoin="round"
            d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733C11.285 4.876 9.623 3.75 7.688 3.75 5.099 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" />
    </svg>`;

    const svgClock = () => `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
         fill="none" stroke="currentColor" stroke-width="1.8"
         class="h-4 w-4 text-slate-500">
      <path stroke-linecap="round" stroke-linejoin="round"
            d="M12 6v6l4 2m6-2a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z" />
    </svg>`;

    const svgChevronLeft = () => `
    <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
    </svg>`;

    const svgChevronRight = () => `
    <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
    </svg>`;

    // ===== Rendering =====
    const statusBadge = (status) => {
        switch (status) {
            case 'LIVE':
                return {
                    label: '진행 중',
                    cls:
                        'inline-flex items-center rounded-full border px-3 py-1 text-xs font-black ' +
                        'border-emerald-200 bg-emerald-50 text-emerald-700',
                };
            case 'DRAFT':
                return {
                    label: '임시 저장',
                    cls:
                        'inline-flex items-center rounded-full border px-3 py-1 text-xs font-black ' +
                        'border-slate-200 bg-slate-50 text-slate-700',
                };
            case 'ENDED_SOLD':
                return {
                    label: '낙찰 완료',
                    cls:
                        'inline-flex items-center rounded-full border px-3 py-1 text-xs font-black ' +
                        'border-slate-300 bg-slate-100 text-slate-900',
                };
            case 'ENDED_UNSOLD':
                return {
                    label: '유찰',
                    cls:
                        'inline-flex items-center rounded-full border px-3 py-1 text-xs font-black ' +
                        'border-rose-200 bg-rose-50 text-rose-700',
                };
            default:
                return {
                    label: String(status ?? '-'),
                    cls:
                        'inline-flex items-center rounded-full border px-3 py-1 text-xs font-black ' +
                        'border-slate-200 bg-white text-slate-700',
                };
        }
    };

    // 행 클릭 목적지
    const resolveRowHref = (item) => {
        const auctionId = item?.auctionId;
        if (!auctionId) return null;
        if (item?.status === 'DRAFT') return `/seller/auctions/drafts/${auctionId}`;
        return `/auctions/${auctionId}`;
    };

    // 가격 정책
    const resolvePriceMain = (it) => {
        if (it?.status === 'ENDED_SOLD') return `낙찰가 ${formatWon(it?.finalPrice ?? 0)}`;
        if (it?.status === 'DRAFT') return `시작가 ${formatWon(it?.startPrice ?? it?.currentPrice ?? 0)}`;
        if (it?.status === 'ENDED_UNSOLD') return `최종가 ${formatWon(it?.currentPrice ?? 0)}`;
        return formatWon(it?.currentPrice ?? 0);
    };

    const renderRows = (items) => {
        $tbody.innerHTML = '';

        const rows = items.map((it) => {
            const auctionId = it?.auctionId;
            const href = resolveRowHref(it);

            const title = escapeHtml(it?.title ?? '');
            const thumb = it?.thumbnailUrl || '';
            const viewCount = Number(it?.viewCount ?? 0);
            const favoriteCount = Number(it?.favoriteCount ?? 0);
            const bidCount = Number(it?.bidCount ?? 0);

            const st = statusBadge(it?.status);

            const priceMain = resolvePriceMain(it);

            // 일정 표시
            const endAtText = formatKstYmdHm(it?.endAt);

            let scheduleSubHtml = `<span class="text-xs font-semibold text-slate-500">-</span>`;
            if (it?.status === 'LIVE') {
                const remaining =
                    it?.remainingTime && String(it.remainingTime).trim()
                        ? String(it.remainingTime).trim()
                        : '-';
                scheduleSubHtml = `
          <span class="inline-flex items-center gap-1 text-xs font-semibold text-slate-500">
            ${svgClock()}
            <span>${escapeHtml(remaining)}</span>
          </span>
        `;
            } else if (it?.status === 'DRAFT') {
                scheduleSubHtml = `<span class="text-xs font-semibold text-slate-500">임시 저장</span>`;
            } else {
                scheduleSubHtml = `<span class="text-xs font-semibold text-slate-500">종료</span>`;
            }

            const thumbHtml = thumb
                ? `<img src="${thumb}" alt="" loading="lazy" class="h-full w-full object-cover" referrerpolicy="no-referrer"
             onerror="this.remove(); this.parentElement.innerHTML =
              '<div class=\\'grid h-full w-full place-items-center text-[10px] font-black tracking-widest text-slate-400\\'>IMAGE</div>';">`
                : `<div class="grid h-full w-full place-items-center text-[10px] font-black tracking-widest text-slate-400">IMAGE</div>`;

            // 우측: 유찰만 재등록 버튼
            let rightCellHtml = `<td class="w-28 px-4 py-4 align-middle text-right"></td>`;
            if (it?.status === 'ENDED_UNSOLD' && auctionId) {
                rightCellHtml = `
          <td class="w-28 px-4 py-4 align-middle text-right">
            <button type="button"
              class="sa-relist-btn whitespace-nowrap inline-flex items-center rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-extrabold text-slate-900 hover:bg-slate-50"
              data-auction-id="${escapeHtml(auctionId)}">
              재등록
            </button>
          </td>
        `;
            }

            // 행 클릭 가능(전 상태)
            const trAttrs = href ? `data-href="${escapeHtml(href)}" role="link" tabindex="0"` : '';
            const trClass =
                'border-b border-slate-100 last:border-b-0 cursor-pointer hover:bg-slate-50';

            return `
        <tr ${trAttrs} class="${trClass}">
          <td class="px-4 py-4 align-middle">
            <div class="flex items-center gap-4">
              <div class="h-[72px] w-[72px] shrink-0 overflow-hidden rounded-xl border border-slate-200 bg-slate-50">
                ${thumbHtml}
              </div>
              <div class="min-w-0">
                <div class="font-extrabold text-slate-900 truncate">${title}</div>
                <div class="mt-1 inline-flex items-center gap-2 text-xs font-semibold text-slate-500">
                  <span class="text-slate-300">·</span>
                  <span class="inline-flex items-center gap-1">
                    ${svgHeart()}
                    <span>${Number.isFinite(favoriteCount) ? favoriteCount.toLocaleString('ko-KR') : '0'}</span>
                  </span>
                </div>
              </div>
            </div>
          </td>

          <td class="px-4 py-4 align-middle">
            <span class="${st.cls}">${st.label}</span>
          </td>

          <td class="px-4 py-4 align-middle">
            <div class="font-extrabold text-slate-900">${escapeHtml(priceMain)}</div>
            <div class="mt-1 text-xs font-semibold text-slate-500">
              입찰 ${Number.isFinite(bidCount) ? bidCount.toLocaleString('ko-KR') : '0'}회
            </div>
          </td>

          <td class="px-4 py-4 align-middle">
            <div class="font-semibold text-slate-700">${escapeHtml(endAtText)}</div>
            <div class="mt-1">${scheduleSubHtml}</div>
          </td>

          ${rightCellHtml}
        </tr>
      `;
        });

        $tbody.insertAdjacentHTML('beforeend', rows.join(''));
    };

    const updateRangeText = ({ total, start, end }) => {
        if (total <= 0) {
            $rangeText.textContent = '전체 0개 중 0-0 표시';
            return;
        }
        $rangeText.textContent = `전체 ${total.toLocaleString('ko-KR')}개 중 ${start.toLocaleString(
            'ko-KR'
        )}-${end.toLocaleString('ko-KR')} 표시`;
    };

    // 경매 목록과 동일한 “룩앤필” 페이지네이션 (상태 이동은 버튼)
    const renderPagination = ({ currentPage, totalPages, hasPrevious, hasNext }, onMove) => {
        $paginationNav.innerHTML = '';
        if (!Number.isFinite(totalPages) || totalPages <= 1) return;

        const mkNavBtn = ({ type, disabled, onClick }) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className =
                'inline-flex h-10 w-10 items-center justify-center rounded-lg border border-gray-300 bg-white text-sm font-semibold text-gray-700' +
                (disabled ? ' opacity-40 cursor-not-allowed' : ' hover:bg-gray-50');
            btn.disabled = Boolean(disabled);
            if (!disabled) btn.addEventListener('click', onClick);
            btn.innerHTML = type === 'prev' ? svgChevronLeft() : svgChevronRight();
            return btn;
        };

        const mkPageBtn = ({ page, active }) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className =
                'inline-flex h-10 w-10 items-center justify-center rounded-lg border border-gray-300 text-sm font-semibold ' +
                (active ? 'bg-slate-900 text-white' : 'bg-white text-gray-700 hover:bg-gray-50');
            btn.textContent = String(page + 1);
            btn.addEventListener('click', () => onMove(page));
            return btn;
        };

        // 이전
        $paginationNav.appendChild(
            mkNavBtn({
                type: 'prev',
                disabled: !hasPrevious,
                onClick: () => onMove(currentPage - 1),
            })
        );

        // 페이지 번호 전체 렌더(요청 방식)
        const pagesWrap = document.createElement('div');
        pagesWrap.className = 'flex gap-1';
        for (let i = 0; i < totalPages; i++) {
            pagesWrap.appendChild(mkPageBtn({ page: i, active: i === currentPage }));
        }
        $paginationNav.appendChild(pagesWrap);

        // 다음
        $paginationNav.appendChild(
            mkNavBtn({
                type: 'next',
                disabled: !hasNext,
                onClick: () => onMove(currentPage + 1),
            })
        );
    };

    // ===== Row interactions =====
    const bindRowInteractions = () => {
        // 행 클릭 이동
        $tbody.addEventListener('click', (e) => {
            // 재등록 버튼은 별도 처리
            const relistBtn = e.target.closest('.sa-relist-btn');
            if (relistBtn) return;

            const tr = e.target.closest('tr[data-href]');
            if (!tr) return;

            // 내부 클릭 예외(혹시 다른 버튼/폼 생겨도 안전)
            if (e.target.closest('a,button,input,select,textarea,label')) return;

            const href = tr.dataset.href;
            if (href) window.location.href = href;
        });

        // 키보드 이동 (Enter/Space)
        $tbody.addEventListener('keydown', (e) => {
            const tr = e.target.closest('tr[data-href]');
            if (!tr) return;

            if (e.key === 'Enter' || e.key === ' ') {
                // 버튼 포커스 상태에서 Space 눌렀을 때 중복 방지
                if (e.target.closest('button')) return;

                e.preventDefault();
                const href = tr.dataset.href;
                if (href) window.location.href = href;
            }
        });

        // 유찰 재등록 버튼 (이벤트 위임)
        $tbody.addEventListener('click', async (e) => {
            const btn = e.target.closest('.sa-relist-btn');
            if (!btn) return;

            e.preventDefault();
            e.stopPropagation();

            const auctionId = btn.dataset.auctionId;
            if (!auctionId) return;

            // 더블클릭 방지
            if (btn.disabled) return;
            const prevText = btn.textContent;
            btn.disabled = true;
            btn.classList.add('opacity-60', 'cursor-not-allowed');
            btn.textContent = '처리중';

            try {
                const draftAuctionId = await apiRelist(auctionId);
                window.location.href = `/seller/auctions/drafts/${encodeURIComponent(draftAuctionId)}`;
            } catch (err) {
                console.error(err);
                btn.disabled = false;
                btn.classList.remove('opacity-60', 'cursor-not-allowed');
                btn.textContent = prevText;
                window.alert('재등록에 실패했습니다. 잠시 후 다시 시도해주세요.');
            }
        });
    };

    // ===== Loaders =====
    const loadCountsAndSummary = async () => {
        const [all, live, closed, ended, draft] = await Promise.all([
            fetchCount(null),
            fetchCount('LIVE'),
            fetchCount('ENDED_SOLD'),
            fetchCount('ENDED_UNSOLD'),
            fetchCount('DRAFT'),
        ]);

        // 탭 카운트
        $tabAllCount.textContent = String(all);
        $tabLiveCount.textContent = String(live);
        $tabClosedCount.textContent = String(closed);
        $tabEndedCount.textContent = String(ended);
        $tabDraftCount.textContent = String(draft);

        // 요약 카드
        $sumLive.textContent = String(live);
        $sumClosed.textContent = String(closed);
        $sumDraft.textContent = String(draft);

        // 총 수익
        const revenue = await fetchRevenue().catch(() => 0);
        $sumRevenue.textContent = formatWon(revenue);
    };

    const loadList = async () => {
        const status = tabToStatus[state.activeTab];

        try {
            // 검색어가 있으면: 0페이지에서 크게 받아서 클라에서 필터 + 클라 페이지네이션
            if (state.search && state.search.trim().length > 0) {
                const data = await apiGetAuctions({ page: 0, size: 200, sort: state.sort, status });
                const raw = Array.isArray(data?.content) ? data.content : [];

                const q = state.search.trim().toLowerCase();
                const filtered = raw.filter((it) =>
                    String(it?.title ?? '').toLowerCase().includes(q)
                );

                const total = filtered.length;
                const totalPages = Math.max(1, Math.ceil(total / state.size));
                const currentPage = Math.min(state.page, totalPages - 1);
                const slice = filtered.slice(
                    currentPage * state.size,
                    currentPage * state.size + state.size
                );

                if (slice.length === 0) {
                    renderRows([]);
                    showEmpty(state.activeTab);
                    updateRangeText({ total: 0, start: 0, end: 0 });
                    $paginationNav.innerHTML = '';
                    return;
                }

                hideEmpty();
                renderRows(slice);

                const start = currentPage * state.size + 1;
                const end = currentPage * state.size + slice.length;
                updateRangeText({ total, start, end });

                renderPagination(
                    {
                        currentPage,
                        totalPages,
                        hasPrevious: currentPage > 0,
                        hasNext: currentPage < totalPages - 1,
                    },
                    (p) => {
                        state.page = p;
                        loadList();
                    }
                );

                return;
            }

            // 검색어 없으면: 서버 페이지네이션 그대로 사용
            const data = await apiGetAuctions({
                page: state.page,
                size: state.size,
                sort: state.sort,
                status,
            });
            const items = Array.isArray(data?.content) ? data.content : [];
            const pageInfo = data?.pageInfo ?? {};
            const total = Number(pageInfo?.totalElements ?? 0);

            if (items.length === 0) {
                renderRows([]);
                showEmpty(state.activeTab);
                updateRangeText({ total: 0, start: 0, end: 0 });
                $paginationNav.innerHTML = '';
                return;
            }

            hideEmpty();
            renderRows(items);

            const currentPage = Number(pageInfo?.currentPage ?? 0);
            const totalPages = Number(pageInfo?.totalPages ?? 1);

            const start = currentPage * state.size + 1;
            const end = currentPage * state.size + items.length;
            updateRangeText({ total, start, end });

            renderPagination(
                {
                    currentPage,
                    totalPages,
                    hasPrevious: Boolean(pageInfo?.hasPrevious),
                    hasNext: Boolean(pageInfo?.hasNext),
                },
                (p) => {
                    state.page = p;
                    loadList();
                }
            );
        } catch (e) {
            console.error(e);
            renderRows([]);
            $emptyTitle.textContent = '목록을 불러오지 못했습니다.';
            $emptyDesc.textContent = '잠시 후 다시 시도해주세요.';
            $empty.classList.remove('hidden');
            updateRangeText({ total: 0, start: 0, end: 0 });
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
        bindRowInteractions();

        await loadCountsAndSummary().catch((e) => console.error(e));
        await loadList();
    };

    document.addEventListener('DOMContentLoaded', init);
})();
