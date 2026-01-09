(() => {
    const ns = (window.EolmagoAuctionDraft = window.EolmagoAuctionDraft || {});

    const $ = (sel, el = document) => el.querySelector(sel);
    const $$ = (sel, el = document) => Array.from(el.querySelectorAll(sel));

    function uuid() {
        if (window.crypto && typeof window.crypto.randomUUID === "function") return window.crypto.randomUUID();
        return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    }

    function escapeHtml(s) {
        return String(s ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function normalizeExtFromMime(file) {
        const t = (file?.type || "").toLowerCase();
        if (t.includes("jpeg") || t.includes("jpg")) return "jpg";
        if (t.includes("png")) return "png";
        if (t.includes("webp")) return "webp";
        return "jpg";
    }

    function extFromUrl(url) {
        try {
            const u = new URL(url);
            const last = (u.pathname.split("/").pop() || "").toLowerCase();
            const m = last.match(/\.([a-z0-9]+)$/);
            if (!m) return "jpg";
            const ext = m[1];
            if (["jpg", "jpeg"].includes(ext)) return "jpg";
            if (["png", "webp"].includes(ext)) return ext;
            return "jpg";
        } catch {
            return "jpg";
        }
    }

    function numericOnly(str) {
        const raw = String(str || "").replace(/[^\d]/g, "");
        return raw ? Number(raw) : null;
    }

    function numericValue(str) {
        const raw = String(str || "").replace(/[^\d]/g, "");
        return raw ? Number(raw) : null;
    }

    function fmtMoney(n) {
        if (n === null || n === undefined || n === "") return "-";
        const num = typeof n === "number" ? n : Number(String(n).replace(/[^\d]/g, ""));
        if (!Number.isFinite(num)) return "-";
        return num.toLocaleString("ko-KR") + "원";
    }

    function formatKoreanDate(dt) {
        const yyyy = dt.getFullYear();
        const mm = String(dt.getMonth() + 1).padStart(2, "0");
        const dd = String(dt.getDate()).padStart(2, "0");
        const hh = String(dt.getHours()).padStart(2, "0");
        const mi = String(dt.getMinutes()).padStart(2, "0");
        return `${yyyy}년 ${mm}월 ${dd}일 ${hh}:${mi}`;
    }

    function durationLabel(hoursStr) {
        const h = Number(hoursStr || "");
        if (!Number.isFinite(h) || !h) return "-";
        const map = {
            12: "12시간",
            24: "24시간 (1일)",
            48: "48시간 (2일)",
            72: "72시간 (3일)",
            96: "96시간 (4일)",
            120: "120시간 (5일)",
            144: "144시간 (6일)",
            168: "168시간 (7일)",
        };
        return map[h] || `${h}시간`;
    }

    ns.util = {
        $, $$,
        uuid,
        escapeHtml,
        normalizeExtFromMime,
        extFromUrl,
        numericOnly,
        numericValue,
        fmtMoney,
        formatKoreanDate,
        durationLabel,
    };
})();
