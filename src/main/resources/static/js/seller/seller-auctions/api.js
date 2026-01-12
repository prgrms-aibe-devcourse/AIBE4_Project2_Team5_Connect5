(() => {
    'use strict';

    const ns = (window.SellerAuctions = window.SellerAuctions || {});

    const getCsrf = () => {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        if (!token || !header) return null;
        return { header, token };
    };

    const apiGetAuctions = async ({ sellerId, page, size, sort, status }) => {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(size));
        params.set('sortKey', sort || 'latest');
        params.set('userId', sellerId);
        if (status) params.set('status', status);

        const res = await fetch(`/api/auctions/list?${params.toString()}`, {
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

    const fetchCount = async ({ sellerId, status }) => {
        const data = await apiGetAuctions({ sellerId, page: 0, size: 1, sort: 'latest', status });
        return Number(data?.pageInfo?.totalElements ?? 0);
    };

    const fetchRevenue = async ({ sellerId }) => {
        let page = 0;
        const size = 100;
        let sum = 0;

        while (true) {
            const data = await apiGetAuctions({
                sellerId,
                page,
                size,
                sort: 'latest',
                status: 'ENDED_SOLD',
            });

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

    ns.api = {
        getCsrf,
        getAuctions: apiGetAuctions,
        relist: apiRelist,
        fetchCount,
        fetchRevenue,
    };
})();
