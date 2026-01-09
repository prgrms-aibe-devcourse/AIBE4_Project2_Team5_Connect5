(() => {
    const ns = (window.EolmagoAuctionDraft = window.EolmagoAuctionDraft || {});
    const { } = ns.util || {};

    async function fetchJson(url, { method = "GET", body } = {}) {
        const headers = {
            Accept: "application/json",
            "Content-Type": "application/json",
        };

        const res = await fetch(url, {
            method,
            headers,
            credentials: "same-origin",
            body: body ? JSON.stringify(body) : undefined,
        });

        const text = await res.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : null;
        } catch {
            data = text || null;
        }

        if (!res.ok) {
            const msg =
                data?.message ||
                data?.error?.message ||
                (typeof data === "string" ? data : null) ||
                `요청 실패 (HTTP ${res.status})`;

            const fieldErrors = data?.errors || data?.fieldErrors || null;
            const err = new Error(msg);
            err.status = res.status;
            err.data = data;
            err.fieldErrors = fieldErrors;
            throw err;
        }
        return data;
    }

    function applyServerFieldErrors(err, showFieldErrorFn) {
        if (typeof showFieldErrorFn !== "function") return;

        const fieldErrors = err?.fieldErrors;

        if (Array.isArray(fieldErrors)) {
            fieldErrors.forEach((fe) => {
                const f = fe.field || fe.name;
                const m = fe.message || fe.defaultMessage || fe.reason;
                if (f && m) showFieldErrorFn(f, m);
            });
            return;
        }

        if (fieldErrors && typeof fieldErrors === "object") {
            Object.entries(fieldErrors).forEach(([f, m]) => {
                if (Array.isArray(m)) showFieldErrorFn(f, m[0]);
                else showFieldErrorFn(f, String(m));
            });
            return;
        }

        const br = err?.data?.bindingResult;
        if (Array.isArray(br)) {
            br.forEach((fe) => {
                if (fe.field && fe.defaultMessage) showFieldErrorFn(fe.field, fe.defaultMessage);
            });
        }
    }

    ns.api = {
        fetchJson,
        applyServerFieldErrors,
        async createDraft(apiBase, payload) {
            return fetchJson(`${apiBase}/drafts`, { method: "POST", body: payload });
        },
        async updateDraft(apiBase, auctionId, payload) {
            return fetchJson(`${apiBase}/drafts/${auctionId}`, { method: "PUT", body: payload });
        },
        async getDraft(apiBase, auctionId) {
            return fetchJson(`${apiBase}/drafts/${auctionId}`, { method: "GET" });
        },
        async publish(apiBase, auctionId) {
            return fetchJson(`${apiBase}/${auctionId}/publish`, { method: "POST" });
        },
        async deleteAuction(apiBase, auctionId) {
            return fetchJson(`${apiBase}/${auctionId}`, { method: "DELETE" });
        },
    };
})();
