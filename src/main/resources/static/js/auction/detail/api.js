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

        // serverNow 우선순위: body(serverNow/serverNowMs) > header(Date) > Date.now()
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

        // epoch string (ms)
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

        if (brand) {
            params.set("brands", brand);
        }

        if (category) {
            params.set("category", category);
        }

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
}
