export class Api {
    async getAuctionDetail(auctionId) {
        // 프로젝트 실제 엔드포인트에 맞게 필요 시 수정
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

    /**
     * 기존 코드(또는 다른 파일)에서 fetchDetailWithServerTime(...)를 호출하는 경우를 위한 하위 호환 메서드입니다.
     * - 반환: { data, response, serverNowMs }
     */
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

    /**
     * 실시간 인기 경매 목록 조회
     * @param {number} page - 페이지 번호 (기본값: 0)
     * @param {number} size - 페이지 크기 (기본값: 12)
     * @returns {Promise<{data: any, response: Response}>}
     */
    async getPopularAuctions(page = 0, size = 12) {
        const params = new URLSearchParams({
            page: String(page),
            size: String(size),
            sort: "popular",
            status: "LIVE"
        });

        const url = `/api/auctions?${params.toString()}`;

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
