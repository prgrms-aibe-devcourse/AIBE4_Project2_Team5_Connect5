(() => {
    'use strict';

    const ns = (window.SellerAuctions = window.SellerAuctions || {});

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

    // ===== Rendering helpers =====
    const statusBadge = (item) => {
        const status = item?.status;
        const endReason = item?.endReason;

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
                if (endReason === 'SELLER_STOPPED') {
                    return {
                        label: '경매 취소',
                        cls:
                            'inline-flex items-center rounded-full border px-3 py-1 text-xs font-black ' +
                            'border-slate-300 bg-slate-100 text-slate-700',
                    };
                }
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

    const resolveRowHref = (item) => {
        const auctionId = item?.auctionId;
        if (!auctionId) return null;
        if (item?.status === 'DRAFT') return `/seller/auctions/drafts/${auctionId}`;
        return `/auctions/${auctionId}`;
    };

    const resolvePriceMain = (it) => {
        if (it?.status === 'ENDED_SOLD') return `낙찰가 ${formatWon(it?.finalPrice ?? 0)}`;
        if (it?.status === 'DRAFT') return `시작가 ${formatWon(it?.startPrice ?? it?.currentPrice ?? 0)}`;
        if (it?.status === 'ENDED_UNSOLD') return `최종가 ${formatWon(it?.currentPrice ?? 0)}`;
        return formatWon(it?.currentPrice ?? 0);
    };

    const renderRows = ({ tbodyEl, items }) => {
        tbodyEl.innerHTML = '';

        const rows = items.map((it) => {
            const auctionId = it?.auctionId;
            const href = resolveRowHref(it);

            const title = escapeHtml(it?.title ?? '');
            const thumb = it?.thumbnailUrl || '';
            const favoriteCount = Number(it?.favoriteCount ?? 0);
            const bidCount = Number(it?.bidCount ?? 0);

            const st = statusBadge(it);
            const priceMain = resolvePriceMain(it);

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

            let rightCellHtml = `<td class="w-28 px-4 py-4 align-middle text-right"></td>`;
            // 재등록 버튼은 유찰(NO_BIDS)일 때만 표시
            if (it?.status === 'ENDED_UNSOLD' && it?.endReason === 'NO_BIDS' && auctionId) {
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

        tbodyEl.insertAdjacentHTML('beforeend', rows.join(''));
    };

    const updateRangeText = ({ rangeTextEl, total, start, end }) => {
        if (total <= 0) {
            rangeTextEl.textContent = '전체 0개 중 0-0 표시';
            return;
        }
        rangeTextEl.textContent = `전체 ${total.toLocaleString('ko-KR')}개 중 ${start.toLocaleString(
            'ko-KR'
        )}-${end.toLocaleString('ko-KR')} 표시`;
    };

    const renderPagination = ({ paginationEl, pageInfo }, onMove) => {
        paginationEl.innerHTML = '';
        const { currentPage, totalPages, hasPrevious, hasNext } = pageInfo;

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

        paginationEl.appendChild(
            mkNavBtn({
                type: 'prev',
                disabled: !hasPrevious,
                onClick: () => onMove(currentPage - 1),
            })
        );

        const pagesWrap = document.createElement('div');
        pagesWrap.className = 'flex gap-1';
        for (let i = 0; i < totalPages; i++) {
            pagesWrap.appendChild(mkPageBtn({ page: i, active: i === currentPage }));
        }
        paginationEl.appendChild(pagesWrap);

        paginationEl.appendChild(
            mkNavBtn({
                type: 'next',
                disabled: !hasNext,
                onClick: () => onMove(currentPage + 1),
            })
        );
    };

    const bindRowInteractions = ({ tbodyEl, onRelist }) => {
        // 행 클릭 이동
        tbodyEl.addEventListener('click', (e) => {
            const relistBtn = e.target.closest('.sa-relist-btn');
            if (relistBtn) return;

            const tr = e.target.closest('tr[data-href]');
            if (!tr) return;

            if (e.target.closest('a,button,input,select,textarea,label')) return;

            const href = tr.dataset.href;
            if (href) window.location.href = href;
        });

        // 키보드 이동 (Enter/Space)
        tbodyEl.addEventListener('keydown', (e) => {
            const tr = e.target.closest('tr[data-href]');
            if (!tr) return;

            if (e.key === 'Enter' || e.key === ' ') {
                if (e.target.closest('button')) return;

                e.preventDefault();
                const href = tr.dataset.href;
                if (href) window.location.href = href;
            }
        });

        // 유찰 재등록 버튼
        tbodyEl.addEventListener('click', async (e) => {
            const btn = e.target.closest('.sa-relist-btn');
            if (!btn) return;

            e.preventDefault();
            e.stopPropagation();

            const auctionId = btn.dataset.auctionId;
            if (!auctionId) return;

            if (btn.disabled) return;

            const prevText = btn.textContent;
            btn.disabled = true;
            btn.classList.add('opacity-60', 'cursor-not-allowed');
            btn.textContent = '처리중';

            try {
                await onRelist(auctionId);
            } catch (err) {
                console.error(err);
                btn.disabled = false;
                btn.classList.remove('opacity-60', 'cursor-not-allowed');
                btn.textContent = prevText;
                window.alert('재등록에 실패했습니다. 잠시 후 다시 시도해주세요.');
            }
        });
    };

    ns.list = {
        escapeHtml,
        formatWon,
        formatKstYmdHm,
        renderRows,
        updateRangeText,
        renderPagination,
        bindRowInteractions,
    };
})();
