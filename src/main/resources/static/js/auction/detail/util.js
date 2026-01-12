export function safeText(v, fallback = "-") {
    if (v === null || v === undefined) return fallback;
    const s = String(v).trim();
    return s ? s : fallback;
}

export function formatNumber(n) {
    const x = Number(n);
    if (Number.isNaN(x)) return "0";
    return x.toLocaleString("ko-KR");
}

export function shortUuid(v) {
    const s = safeText(v, "");
    if (!s) return "-";
    return s.replaceAll("-", "").slice(0, 8);
}

export function parseOffsetDateTimeToMs(v) {
    if (!v) return null;
    const t = Date.parse(v);
    return Number.isNaN(t) ? null : t;
}

export function formatEndAt(v) {
    const ms = parseOffsetDateTimeToMs(v);
    if (!ms) return "-";
    const d = new Date(ms);

    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    const hh = String(d.getHours()).padStart(2, "0");
    const mi = String(d.getMinutes()).padStart(2, "0");

    return `${yyyy}.${mm}.${dd} ${hh}:${mi}`;
}

export function formatRemainingHms(diffMs) {
    const total = Math.max(0, Math.floor(diffMs / 1000));
    const h = String(Math.floor(total / 3600)).padStart(2, "0");
    const m = String(Math.floor((total % 3600) / 60)).padStart(2, "0");
    const s = String(total % 60).padStart(2, "0");
    return `${h}:${m}:${s}`;
}

export function startCountdown({ endAtMs, getNowMs, onTick, onDone }) {
    let timer = null;

    const tick = () => {
        const now = getNowMs();
        const diff = endAtMs - now;
        if (diff <= 0) {
            onTick?.(0);
            onDone?.();
            stop();
            return;
        }
        onTick?.(diff);
    };

    const start = () => {
        tick();
        timer = setInterval(tick, 250);
    };

    const stop = () => {
        if (timer) clearInterval(timer);
        timer = null;
    };

    start();
    return stop;
}

export function computeOffsetMsFromServerNow(serverNowMs) {
    const now = Date.now();
    const s = Number(serverNowMs);
    if (!Number.isFinite(s) || s <= 0) return 0;
    return s - now;
}

/**
 * 서버 시간 확보 우선순위:
 * 1) data.serverNow (ISO/epoch 둘 다 지원)
 * 2) response header Date
 * 3) Date.now()
 */
export function parseServerNowMsFromResponse(data, response) {
    // 1) body
    const bodyVal = data?.serverNow ?? data?.serverNowMs ?? null;
    const bodyMs = toMs(bodyVal);
    if (bodyMs) return bodyMs;

    // 2) header Date
    const headerDate = response?.headers?.get?.("Date") ?? null;
    const headerMs = toMs(headerDate);
    if (headerMs) return headerMs;

    // 3) fallback
    return Date.now();
}

function toMs(v) {
    if (v === null || v === undefined) return null;
    if (typeof v === "number") return Number.isFinite(v) ? v : null;

    const s = String(v).trim();
    if (!s) return null;

    // epoch string
    if (/^\d{12,}$/.test(s)) {
        const n = Number(s);
        return Number.isFinite(n) ? n : null;
    }

    const t = Date.parse(s);
    return Number.isNaN(t) ? null : t;
}

/**
 * 상태 라벨(필요 시 확장)
 */
export function resolveLabel(kind, value) {
    if (kind === "condition") {
        // 서버 enum/문자열 케이스에 맞게 필요 시 수정
        const map = {
            NEW: "새상품",
            LIKE_NEW: "거의 새것",
            GOOD: "양호",
            FAIR: "보통",
            POOR: "하자 있음"
        };
        return map[value] ?? safeText(value);
    }
    return safeText(value);
}

/**
 * displayOrder 0부터 시작(요청 반영)
 * - 객체 배열: displayOrder 기준 오름차순 정렬
 * - 문자열 배열: 순서대로 displayOrder=idx
 */
export function normalizeOrderedImages(data) {
    const candidates =
        data?.contentImages ??
        data?.detailImages ??
        data?.contentImageList ??
        data?.bodyImages ??
        null;

    // 객체 배열 케이스
    if (Array.isArray(candidates) && candidates.length && typeof candidates[0] === "object") {
        const mapped = candidates
            .map((x) => {
                const url = x.url ?? x.imageUrl ?? x.imageURL ?? x.path ?? x.src ?? null;

                // displayOrder가 0일 수도 있으므로 null/undefined만 "없음"으로 취급
                const orderRaw = x.displayOrder ?? x.order ?? x.display_order ?? null;
                const displayOrder = (orderRaw === null || orderRaw === undefined) ? null : Number(orderRaw);

                return { url, displayOrder };
            })
            .filter((x) => !!x.url);

        return mapped.sort((a, b) => {
            const ao = (a.displayOrder === null || Number.isNaN(a.displayOrder)) ? Number.MAX_SAFE_INTEGER : a.displayOrder;
            const bo = (b.displayOrder === null || Number.isNaN(b.displayOrder)) ? Number.MAX_SAFE_INTEGER : b.displayOrder;
            return ao - bo;
        });
    }

    // 문자열 배열 케이스
    const urls =
        (Array.isArray(data?.contentImageUrls) && data.contentImageUrls) ||
        (Array.isArray(data?.detailImageUrls) && data.detailImageUrls) ||
        (Array.isArray(data?.bodyImageUrls) && data.bodyImageUrls) ||
        [];

    return urls.filter(Boolean).map((url, idx) => ({ url, displayOrder: idx }));
}

/**
 * 서버 bidIncrement가 없을 때 프론트에서 동일 룰로 계산(방어)
 * Java BidIncrementCalculator와 동일
 */
export function calcBidIncrement(currentPrice) {
    const p = Number(currentPrice ?? 0);

    if (p < 200_000) return 1_000;
    if (p < 1_000_000) return 5_000;
    if (p < 3_000_000) return 10_000;
    if (p < 10_000_000) return 50_000;
    if (p <= 30_000_000) return 100_000;
    return 100_000;
}
