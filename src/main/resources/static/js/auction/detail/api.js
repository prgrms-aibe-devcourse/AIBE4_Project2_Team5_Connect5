export class Api {
    async getAuctionDetail(auctionId) {
        const url = `/api/auctions/${encodeURIComponent(auctionId)}`;

        const response = await fetch(url, {
            method: "GET",
            headers: { Accept: "application/json" },
        });

        if (!response.ok) {
            throw new Error(`GET ${url} failed: ${response.status}`);
        }

        const data = await response.json();
        return { data, response };
    }

    async fetchDetailWithServerTime(auctionId) {
        const { data, response } = await this.getAuctionDetail(auctionId);

        const serverNowMs =
            this.#toMs(data?.serverNow ?? data?.serverNowMs) ??
            this.#toMs(response?.headers?.get?.("Date")) ??
            Date.now();

        return { data, response, serverNowMs };
    }

    #toMs(v) {
        if (v === null || v === undefined) return null;

        if (typeof v === "number") {
            return Number.isFinite(v) && v > 0 ? v : null;
        }

        const s = String(v).trim();
        if (!s) return null;

        if (/^\d{12,}$/.test(s)) {
            const n = Number(s);
            return Number.isFinite(n) && n > 0 ? n : null;
        }

        const t = Date.parse(s);
        return Number.isNaN(t) ? null : t;
    }

    async getPopularAuctions(page = 0, size = 12, brand = null, category = null) {
        const params = new URLSearchParams({
            page: String(page),
            size: String(size),
            sortKey: "popular",
            status: "LIVE"
        });

        if (brand) params.set("brands", brand);
        if (category) params.set("category", category);

        const url = `/api/auctions/list?${params.toString()}`;

        const response = await fetch(url, {
            method: "GET",
            headers: { Accept: "application/json" },
        });

        if (!response.ok) {
            throw new Error(`GET ${url} failed: ${response.status}`);
        }

        const data = await response.json();
        return { data, response };
    }

    /**
     * 경매 마감 API 호출 (멱등)
     */
    async closeAuction(auctionId) {
        const url = `/api/auctions/${encodeURIComponent(auctionId)}/close`;

        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            },
            credentials: "same-origin"
        });

        if (!response.ok) {
            if (response.status === 409 || response.status === 404) {
                console.log(`Auction ${auctionId} already closed or not found`);
                return { success: true };
            }
            throw new Error(`POST ${url} failed: ${response.status}`);
        }

        return { success: true };
    }

    /**
     * 거래 생성 (낙찰(ENDED_SOLD & SOLD)일 때만 호출해야 함)
     * request:
     * {
     *   auctionId: UUID,
     *   sellerId: UUID,
     *   buyerId: UUID,
     *   finalPrice: number
     * }
     */
    async createDealFromAuction(request) {
        const url = `/api/deals/from-auction`;

        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            },
            credentials: "same-origin",
            body: JSON.stringify(request)
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            const errorMessage = errorData?.message || `거래 생성에 실패했습니다 (${response.status})`;
            throw new Error(errorMessage);
        }

        const data = await response.json().catch(() => null);
        return { data, response };
    }

    /**
     * 유찰 경매 재등록
     */
    async republishUnsoldAuction(auctionId) {
        const url = `/api/auctions/${encodeURIComponent(auctionId)}/republish`;

        const response = await fetch(url, {
            method: "POST",
            headers: { Accept: "application/json" },
            credentials: "same-origin"
        });

        if (!response.ok) {
            let msg = `재등록에 실패했습니다 (${response.status})`;
            const data = await response.json().catch(() => null);
            if (data?.message) msg = data.message;
            throw new Error(msg);
        }

        const data = await response.json().catch(() => null);
        return { data, response };
    }

    /**
     * 판매자 경매 취소
     */
    async cancelAuctionBySeller(auctionId) {
        const url = `/api/auctions/${encodeURIComponent(auctionId)}/stop`;

        const response = await fetch(url, {
            method: "POST",
            headers: { Accept: "application/json" },
            credentials: "same-origin"
        });

        if (!response.ok) {
            let msg = `경매 취소에 실패했습니다 (${response.status})`;
            const data = await response.json().catch(() => null);
            if (data?.message) msg = data.message;
            throw new Error(msg);
        }

        return { success: true };
    }

    /**
     * 입찰 생성
     */
    async createBid(auctionId, amount, clientRequestId) {
        const url = `/api/auctions/${encodeURIComponent(auctionId)}/bids`;

        const response = await fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            },
            credentials: "same-origin",
            body: JSON.stringify({
                amount: amount,
                clientRequestId: clientRequestId
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            const errorMessage = errorData?.message || `입찰에 실패했습니다 (${response.status})`;
            throw new Error(errorMessage);
        }

        const data = await response.json();
        return { data, response };
    }
}
