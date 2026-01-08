(() => {
    const $ = (sel, el = document) => el.querySelector(sel);
    const $$ = (sel, el = document) => Array.from(el.querySelectorAll(sel));

    const root = $("#draftPage");
    if (!root) return;

    // -------------------------
    // Page config
    // -------------------------
    const apiBase = root.dataset.apiBase || "/api/auctions";
    let auctionId = root.dataset.auctionId || ""; // UUID
    const mode = root.dataset.mode || "create";

    const redirectAfterPublish = root.dataset.redirectAfterPublish || "/seller/auctions";
    const redirectAfterDelete = root.dataset.redirectAfterDelete || "/seller/auctions";

    const supabaseUrl = (root.dataset.supabaseUrl || "").trim();
    const supabaseAnonKey = (root.dataset.supabaseAnonKey || "").trim();
    const supabaseBucket = (root.dataset.supabaseBucket || "eolmago").trim();

    // -------------------------
    // Elements
    // -------------------------
    // Fields
    const elItemName = $("#itemName");
    const elCategory = $("#category");
    const elCondition = $("#condition");
    const elTitle = $("#title");
    const elDesc = $("#description");
    const elStartPrice = $("#startPrice");
    const elDuration = $("#durationHours");

    // Required specs
    const elBrandSelect = $("#brandSelect");
    const elBrandCustomWrap = $("#brandCustomWrap");
    const elBrandCustomInput = $("#brandCustomInput");
    const elStorageGb = $("#storageGb");

    // Preview
    const elEndAtPreview = $("#endAtPreview");

    // Buttons
    const elSaveBtn = $("#saveDraftBtn");
    const elPublishBtn = $("#publishBtn");
    const elDeleteBtn = $("#deleteBtn");

    // Alert
    const elAlert = $("#formAlert");

    // Summary
    const elSummaryItemName = $("#summaryItemName");
    const elSummaryStartPrice = $("#summaryStartPrice");
    const elSummaryDuration = $("#summaryDuration");
    const elSummaryImages = $("#summaryImages");

    // Counters
    const titleCount = $("#titleCount");
    const descCount = $("#descCount");

    // Images UI
    const imageInput = $("#imageInput");
    const dropzone = $("#dropzone");
    const thumbGrid = $("#thumbGrid");
    const imageCountEl = $("#imageCount");

    // Modal (URL add)
    const imageUrlModal = $("#imageUrlModal");
    const imageUrlInput = $("#imageUrlInput");
    const imageUrlError = $("#imageUrlError");
    const addImageUrlBtn = $("#addImageUrlBtn");
    const confirmAddImageUrlBtn = $("#confirmAddImageUrlBtn");

    // Toast
    const toast = $("#toast");
    const toastTitle = $("#toastTitle");
    const toastMsg = $("#toastMsg");

    // Publish retry box
    const publishRetryBox = $("#publishRetryBox");
    const retryPublishBtn = $("#retryPublishBtn");

    // Status
    const statusTextEl = $("#statusText");

    // -------------------------
    // State
    // -------------------------
    const MAX_IMAGES = 10;
    const DND_TYPE = "application/x-eolmago-image-index";

    /**
     * images item shape:
     * - type: 'file' | 'url'
     * - For file: file, previewUrl, ext, url(after upload), storagePath
     * - For url: url, storagePath(if supabase public url), isExternal
     */
    const images = [];

    // Dirty flag
    let isDirty = false;

    // Prevent double submit
    let isSaving = false;
    let isPublishing = false;

    // Supabase client (optional)
    const supabaseClient = (() => {
        if (!supabaseUrl || !supabaseAnonKey) return null;
        const lib = window.supabase;
        if (!lib || typeof lib.createClient !== "function") return null;
        return lib.createClient(supabaseUrl, supabaseAnonKey);
    })();

    // Public URL prefix for path parsing
    const supabasePublicPrefix =
        supabaseClient && supabaseUrl
            ? `${supabaseUrl.replace(/\/$/, "")}/storage/v1/object/public/${supabaseBucket}/`
            : "";

    // -------------------------
    // UI helpers
    // -------------------------
    function setDirty(v = true) {
        isDirty = v;
    }

    function showAlert(msg) {
        if (!elAlert) return;
        elAlert.textContent = msg;
        elAlert.classList.remove("hidden");
        elAlert.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function hideAlert() {
        if (!elAlert) return;
        elAlert.classList.add("hidden");
        elAlert.textContent = "";
    }

    function showFieldError(name, msg) {
        const el = document.querySelector(`[data-error="${name}"]`);
        if (!el) return;
        el.textContent = msg;
        el.classList.remove("hidden");
    }

    function clearFieldErrors() {
        $$(`[data-error]`).forEach((el) => {
            el.textContent = "";
            el.classList.add("hidden");
        });
    }

    function fmtMoney(n) {
        if (n === null || n === undefined || n === "") return "-";
        const num = typeof n === "number" ? n : Number(String(n).replace(/[^\d]/g, ""));
        if (!Number.isFinite(num)) return "-";
        return num.toLocaleString("ko-KR") + "원";
    }

    function setToast(title, msg) {
        if (!toast) return;
        toastTitle.textContent = title;
        toastMsg.textContent = msg;
        toast.classList.remove("hidden");
        window.clearTimeout(setToast._t);
        setToast._t = window.setTimeout(() => toast.classList.add("hidden"), 2600);
    }

    function setButtonsState() {
        const canPublish = Boolean(auctionId);
        const canDelete = Boolean(auctionId);

        if (elPublishBtn) elPublishBtn.disabled = !canPublish;
        if (elDeleteBtn) elDeleteBtn.disabled = !canDelete;

        if (statusTextEl) statusTextEl.textContent = auctionId ? "임시 저장" : "작성 중";
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

    function parseSupabasePathFromPublicUrl(url) {
        if (!supabasePublicPrefix) return null;
        if (!url.startsWith(supabasePublicPrefix)) return null;
        return url.substring(supabasePublicPrefix.length);
    }

    // -------------------------
    // Counters
    // -------------------------
    function bindCounters() {
        const update = () => {
            if (titleCount) titleCount.textContent = String(elTitle?.value?.length || 0);
            if (descCount) descCount.textContent = String(elDesc?.value?.length || 0);
        };
        [elTitle, elDesc].forEach((el) => el?.addEventListener("input", update));
        update();
    }

    // -------------------------
    // Money input
    // -------------------------
    function bindMoneyInput() {
        if (!elStartPrice) return;
        const format = () => {
            const raw = elStartPrice.value.replace(/[^\d]/g, "");
            if (!raw) {
                elStartPrice.value = "";
                return;
            }
            const num = Number(raw);
            elStartPrice.value = Number.isFinite(num) ? num.toLocaleString("ko-KR") : "";
        };
        elStartPrice.addEventListener("input", () => {
            format();
            setDirty(true);
            syncSummary();
        });
        elStartPrice.addEventListener("blur", format);
    }

    // -------------------------
    // Duration default + end preview
    // -------------------------
    function formatKoreanDate(dt) {
        const yyyy = dt.getFullYear();
        const mm = String(dt.getMonth() + 1).padStart(2, "0");
        const dd = String(dt.getDate()).padStart(2, "0");
        const hh = String(dt.getHours()).padStart(2, "0");
        const mi = String(dt.getMinutes()).padStart(2, "0");
        return `${yyyy}년 ${mm}월 ${dd}일 ${hh}:${mi}`;
    }

    function updateEndPreview() {
        if (!elEndAtPreview) return;
        const hours = Number(elDuration?.value || "");
        if (!hours || !Number.isFinite(hours)) {
            elEndAtPreview.textContent = "기간을 선택하면 표시됩니다";
            return;
        }
        const now = new Date();
        const end = new Date(now.getTime() + hours * 60 * 60 * 1000);
        elEndAtPreview.textContent = `${formatKoreanDate(end)} 종료`;
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

    function bindDurationPreview() {
        elDuration?.addEventListener("change", () => {
            updateEndPreview();
            syncSummary();
            setDirty(true);
        });
        updateEndPreview();
    }

    function applyDefaultDurationIfCreate() {
        // ✅ 임시저장 화면 처음 진입 시 기본 24시간
        if (auctionId) return; // edit/load 상태면 건드리지 않음
        if (!elDuration) return;
        if (String(elDuration.value || "").trim()) return;
        elDuration.value = "24";
        updateEndPreview();
        syncSummary();
    }

    // -------------------------
    // Required specs (brand + storageGb)
    // -------------------------
    function enableSpecs(enabled) {
        if (elBrandSelect) elBrandSelect.disabled = !enabled;
        if (elStorageGb) elStorageGb.disabled = !enabled;

        // category 없으면 커스텀 입력도 숨김
        if (!enabled) {
            if (elBrandCustomWrap) elBrandCustomWrap.classList.add("hidden");
            if (elBrandCustomInput) elBrandCustomInput.value = "";
            if (elBrandSelect) elBrandSelect.value = "";
            if (elStorageGb) elStorageGb.value = "";
        }
    }

    function bindSpecs() {
        // category 선택 전에는 비활성
        enableSpecs(Boolean(elCategory?.value));

        elCategory?.addEventListener("change", () => {
            enableSpecs(Boolean(elCategory.value));
            setDirty(true);
        });

        elBrandSelect?.addEventListener("change", () => {
            const v = elBrandSelect.value;
            if (v === "__custom__") {
                elBrandCustomWrap.classList.remove("hidden");
                elBrandCustomInput.focus();
            } else {
                elBrandCustomWrap.classList.add("hidden");
                elBrandCustomInput.value = "";
            }
            setDirty(true);
        });

        // 용량 숫자만
        elStorageGb?.addEventListener("input", () => {
            const raw = String(elStorageGb.value || "").replace(/[^\d]/g, "");
            elStorageGb.value = raw;
            setDirty(true);
        });

        elBrandCustomInput?.addEventListener("input", () => setDirty(true));
    }

    function numericOnly(str) {
        const raw = String(str || "").replace(/[^\d]/g, "");
        return raw ? Number(raw) : null;
    }

    function collectSpecs() {
        const cat = (elCategory?.value || "").trim();
        if (!cat) return null;

        // PHONE/TABLET만 필수 스펙 강제
        const needs = ["PHONE", "TABLET"].includes(cat);
        if (!needs) return null;

        let brand = (elBrandSelect?.value || "").trim();
        if (brand === "__custom__") {
            brand = (elBrandCustomInput?.value || "").trim();
        }
        const storageGb = numericOnly(elStorageGb?.value);

        return {
            brand: brand || null,
            storageGb: storageGb,
        };
    }

    function validateSpecs(payload) {
        const cat = payload.category;
        const needs = ["PHONE", "TABLET"].includes(cat);

        if (!needs) return [];

        const errs = [];
        const brand = payload?.specs?.brand;
        const storageGb = payload?.specs?.storageGb;

        if (!brand || !String(brand).trim()) errs.push(["brand", "브랜드를 입력해주세요."]);
        if (storageGb == null) errs.push(["storageGb", "용량(GB)을 입력해주세요."]);
        if (storageGb != null && (!Number.isFinite(storageGb) || storageGb <= 0)) {
            errs.push(["storageGb", "용량(GB)은 0보다 큰 숫자여야 합니다."]);
        }
        return errs;
    }

    function applySpecsToUI(specs) {
        // specs: { brand, storageGb }
        const brand = (specs?.brand ?? "").toString().trim();
        const storage = specs?.storageGb;

        enableSpecs(Boolean(elCategory?.value));

        // 브랜드가 Apple/Samsung이면 select로, 아니면 custom
        const known = ["Apple", "Samsung"];
        if (brand && known.includes(brand)) {
            elBrandSelect.value = brand;
            elBrandCustomWrap.classList.add("hidden");
            elBrandCustomInput.value = "";
        } else if (brand) {
            elBrandSelect.value = "__custom__";
            elBrandCustomWrap.classList.remove("hidden");
            elBrandCustomInput.value = brand;
        } else {
            elBrandSelect.value = "";
            elBrandCustomWrap.classList.add("hidden");
            elBrandCustomInput.value = "";
        }

        elStorageGb.value = storage != null ? String(storage) : "";
    }

    // -------------------------
    // Images
    // -------------------------
    function updateImageCount() {
        if (imageCountEl) imageCountEl.textContent = String(images.length);
        if (elSummaryImages) elSummaryImages.textContent = String(images.length);
    }

    function ensureImageCapacity(countToAdd) {
        if (images.length + countToAdd > MAX_IMAGES) {
            setToast("이미지 제한", `최대 ${MAX_IMAGES}장까지 등록할 수 있습니다.`);
            return false;
        }
        return true;
    }

    function renderThumbs() {
        if (!thumbGrid) return;
        thumbGrid.innerHTML = "";

        if (images.length === 0) {
            const empty = document.createElement("div");
            empty.className =
                "col-span-full rounded-xl border border-gray-200 bg-white px-4 py-4 text-sm text-gray-600";
            empty.textContent = "아직 등록된 이미지가 없습니다. 최소 1장을 등록해주세요.";
            thumbGrid.appendChild(empty);
            updateImageCount();
            return;
        }

        images.forEach((img, idx) => {
            const card = document.createElement("div");
            card.className = "group relative overflow-hidden rounded-2xl border border-gray-200 bg-white";
            card.draggable = true;
            card.dataset.index = String(idx);

            const src = img.previewUrl || img.url || "";
            const statusLabel =
                img.type === "file" && !img.url ? "업로드 대기" :
                    img.type === "file" && img.url ? "업로드 완료" :
                        img.type === "url" ? "URL" : "";

            card.innerHTML = `
        <div class="relative aspect-square bg-gray-100">
          ${
                src
                    ? `<img src="${escapeHtml(src)}" alt="미리보기" class="h-full w-full object-cover select-none pointer-events-none" />`
                    : `<div class="h-full w-full flex items-center justify-center text-sm text-gray-500">미리보기 없음</div>`
            }

          <!-- 대표 태그(첫번째) -->
          ${
                idx === 0
                    ? `<span class="absolute left-2 top-2 inline-flex items-center rounded-full bg-slate-900/90 px-2.5 py-1 text-[11px] font-semibold text-white">
                   대표
                 </span>`
                    : ``
            }

          <!-- 제거 버튼: 우상단 빨간 X -->
          <button type="button"
                  class="absolute right-2 top-2 inline-flex h-8 w-8 items-center justify-center rounded-full bg-red-600 text-white shadow-sm hover:bg-red-700"
                  data-remove
                  aria-label="이미지 제거">
            <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
              <path d="M18 6 6 18"></path>
              <path d="m6 6 12 12"></path>
            </svg>
          </button>

          <!-- 상태 칩(선택: 너무 시끄러우면 제거 가능) -->
          <span class="absolute left-2 bottom-2 inline-flex items-center rounded-full border border-gray-200 bg-white/90 px-2 py-0.5 text-[11px] font-semibold text-gray-700">
            ${escapeHtml(statusLabel)}
          </span>
        </div>
      `;

            card.querySelector("[data-remove]").addEventListener("click", (e) => {
                e.preventDefault();
                e.stopPropagation();

                if (img.type === "file" && img.previewUrl) {
                    try { URL.revokeObjectURL(img.previewUrl); } catch {}
                }
                images.splice(idx, 1);
                renderThumbs();
                syncSummary();
                setDirty(true);
            });

            // Drag reorder (복제 버그 방지: 내부 타입 사용 + drop 전파 차단)
            card.addEventListener("dragstart", (e) => {
                e.dataTransfer.effectAllowed = "move";
                e.dataTransfer.setData(DND_TYPE, String(idx));
                // 일부 브라우저 호환용(빈 text/plain)
                e.dataTransfer.setData("text/plain", "");
                card.classList.add("opacity-60");
            });

            card.addEventListener("dragend", () => {
                card.classList.remove("opacity-60");
                card.classList.remove("ring-4", "ring-slate-900/10");
            });

            card.addEventListener("dragover", (e) => {
                // 내부 reorder 드래그만 허용
                if (!e.dataTransfer?.types?.includes(DND_TYPE)) return;
                e.preventDefault();
                e.dataTransfer.dropEffect = "move";
                card.classList.add("ring-4", "ring-slate-900/10");
            });

            card.addEventListener("dragleave", () => {
                card.classList.remove("ring-4", "ring-slate-900/10");
            });

            card.addEventListener("drop", (e) => {
                if (!e.dataTransfer?.types?.includes(DND_TYPE)) return;
                e.preventDefault();
                e.stopPropagation(); // ✅ dropzone으로 전파되어 파일로 인식/추가되는 케이스 차단
                card.classList.remove("ring-4", "ring-slate-900/10");

                const from = Number(e.dataTransfer.getData(DND_TYPE));
                const to = idx;
                if (!Number.isFinite(from) || from < 0 || from >= images.length) return;
                if (from === to) return;

                const moved = images.splice(from, 1)[0];
                images.splice(to, 0, moved);

                // ✅ 길이 절대 유지(복제/손실 방지)
                renderThumbs();
                syncSummary();
                setDirty(true);
            });

            thumbGrid.appendChild(card);
        });

        updateImageCount();
    }

    function bindImageUpload() {
        imageInput?.addEventListener("change", () => {
            const files = Array.from(imageInput.files || []);
            if (!files.length) return;
            if (!ensureImageCapacity(files.length)) return;

            files.forEach((f) => {
                images.push({
                    id: crypto.randomUUID(),
                    type: "file",
                    file: f,
                    fileName: f.name,
                    ext: normalizeExtFromMime(f),
                    previewUrl: URL.createObjectURL(f),
                    url: "",
                    storagePath: null,
                });
            });

            renderThumbs();
            syncSummary();
            setDirty(true);
            imageInput.value = "";
        });

        if (!dropzone) return;

        ["dragenter", "dragover"].forEach((evt) =>
            dropzone.addEventListener(evt, (e) => {
                // 내부 reorder 드래그면 dropzone 스타일 반응하지 않게
                if (e.dataTransfer?.types?.includes(DND_TYPE)) return;
                e.preventDefault();
                dropzone.classList.add("border-slate-900", "bg-white");
            })
        );

        ["dragleave", "drop"].forEach((evt) =>
            dropzone.addEventListener(evt, (e) => {
                if (e.dataTransfer?.types?.includes(DND_TYPE)) return;
                e.preventDefault();
                dropzone.classList.remove("border-slate-900", "bg-white");
            })
        );

        dropzone.addEventListener("drop", (e) => {
            // ✅ 내부 reorder drop이면 파일 처리 금지
            if (e.dataTransfer?.types?.includes(DND_TYPE)) return;

            const dt = e.dataTransfer;
            if (!dt?.files?.length) return;

            const files = Array.from(dt.files);
            if (!ensureImageCapacity(files.length)) return;

            files.forEach((f) => {
                images.push({
                    id: crypto.randomUUID(),
                    type: "file",
                    file: f,
                    fileName: f.name,
                    ext: normalizeExtFromMime(f),
                    previewUrl: URL.createObjectURL(f),
                    url: "",
                    storagePath: null,
                });
            });

            renderThumbs();
            syncSummary();
            setDirty(true);
        });
    }

    function openImageUrlModal() {
        if (!imageUrlModal) return;
        imageUrlInput.value = "";
        imageUrlError.classList.add("hidden");
        imageUrlError.textContent = "";
        imageUrlModal.classList.remove("hidden");
    }

    function closeImageUrlModal() {
        imageUrlModal?.classList.add("hidden");
    }

    function bindImageUrlModal() {
        addImageUrlBtn?.addEventListener("click", openImageUrlModal);
        $$("[data-modal-close]").forEach((el) => el.addEventListener("click", closeImageUrlModal));

        confirmAddImageUrlBtn?.addEventListener("click", () => {
            const url = (imageUrlInput?.value || "").trim();
            if (!url) {
                imageUrlError.textContent = "URL을 입력해주세요.";
                imageUrlError.classList.remove("hidden");
                return;
            }
            try {
                new URL(url);
            } catch {
                imageUrlError.textContent = "유효한 URL 형식이 아닙니다.";
                imageUrlError.classList.remove("hidden");
                return;
            }
            if (!ensureImageCapacity(1)) return;

            const storagePath = parseSupabasePathFromPublicUrl(url);
            images.push({
                id: crypto.randomUUID(),
                type: "url",
                url,
                previewUrl: url,
                fileName: url.split("/").pop() || "",
                ext: extFromUrl(url),
                storagePath,
                isExternal: !storagePath,
            });

            renderThumbs();
            syncSummary();
            setDirty(true);
            closeImageUrlModal();
        });
    }

    function collectImageUrls() {
        return images.map((x) => x.url).filter(Boolean);
    }

    function hasPendingFileUploads() {
        return images.some((x) => x.type === "file" && !x.url);
    }

    // -------------------------
    // Summary
    // -------------------------
    function syncSummary() {
        if (elSummaryItemName) elSummaryItemName.textContent = (elItemName?.value || "").trim() || "-";
        if (elSummaryStartPrice) elSummaryStartPrice.textContent = fmtMoney(elStartPrice?.value || "");
        if (elSummaryDuration) elSummaryDuration.textContent = durationLabel(elDuration?.value);
        if (elSummaryImages) elSummaryImages.textContent = String(images.length);
    }

    function bindSummary() {
        [elItemName, elTitle, elStartPrice, elDesc].forEach((el) =>
            el?.addEventListener("input", () => {
                syncSummary();
                setDirty(true);
            })
        );
        [elCategory, elCondition, elDuration].forEach((el) =>
            el?.addEventListener("change", () => {
                syncSummary();
                setDirty(true);
            })
        );
        syncSummary();
    }

    // -------------------------
    // Fetch helpers
    // -------------------------
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

    function applyServerFieldErrors(err) {
        const fieldErrors = err?.fieldErrors;

        if (Array.isArray(fieldErrors)) {
            fieldErrors.forEach((fe) => {
                const f = fe.field || fe.name;
                const m = fe.message || fe.defaultMessage || fe.reason;
                if (f && m) showFieldError(f, m);
            });
            return;
        }

        if (fieldErrors && typeof fieldErrors === "object") {
            Object.entries(fieldErrors).forEach(([f, m]) => {
                if (Array.isArray(m)) showFieldError(f, m[0]);
                else showFieldError(f, String(m));
            });
            return;
        }

        const br = err?.data?.bindingResult;
        if (Array.isArray(br)) {
            br.forEach((fe) => {
                if (fe.field && fe.defaultMessage) showFieldError(fe.field, fe.defaultMessage);
            });
        }
    }

    // -------------------------
    // Validation / payload
    // -------------------------
    function numericValue(str) {
        const raw = String(str || "").replace(/[^\d]/g, "");
        return raw ? Number(raw) : null;
    }

    function collectPayload(imageUrls) {
        const specs = collectSpecs();
        return {
            title: (elTitle?.value || "").trim(),
            description: (elDesc?.value || "").trim() || null,
            startPrice: numericValue(elStartPrice?.value),
            durationHours: elDuration?.value ? Number(elDuration.value) : null,
            itemName: (elItemName?.value || "").trim(),
            category: elCategory?.value || null,
            condition: elCondition?.value || null,
            specs: specs,
            imageUrls: imageUrls ?? collectImageUrls(),
        };
    }

    function validatePayload(payload) {
        const errors = [];

        if (!payload.title?.trim()) errors.push(["title", "제목을 입력해주세요."]);
        if (!payload.itemName?.trim()) errors.push(["itemName", "상품명을 입력해주세요."]);
        if (!payload.category) errors.push(["category", "카테고리를 선택해주세요."]);
        if (!payload.condition) errors.push(["condition", "상품 상태를 선택해주세요."]);

        // ✅ 필수 스펙 검증
        errors.push(...validateSpecs(payload));

        if (payload.startPrice == null) errors.push(["startPrice", "시작가를 입력해주세요."]);
        if (payload.startPrice != null && (payload.startPrice < 10000 || payload.startPrice > 10000000)) {
            errors.push(["startPrice", "시작가는 10,000원 이상 10,000,000원 이하여야 합니다."]);
        }

        if (payload.durationHours == null) errors.push(["durationHours", "경매 기간을 선택해주세요."]);
        if (payload.durationHours != null && (payload.durationHours < 12 || payload.durationHours > 168)) {
            errors.push(["durationHours", "경매 기간은 12~168시간 사이여야 합니다."]);
        }

        if (!payload.imageUrls || payload.imageUrls.length < 1) {
            errors.push(["imageUrls", "이미지를 1장 이상 등록해주세요."]);
        }
        if (payload.imageUrls && payload.imageUrls.length > 10) {
            errors.push(["imageUrls", "이미지는 최대 10장까지 등록할 수 있습니다."]);
        }

        return errors;
    }

    // -------------------------
    // Supabase Storage ops
    // -------------------------
    async function sbUpload(path, file, contentType, upsert = true) {
        if (!supabaseClient) throw new Error("Supabase 설정이 없습니다(supabaseUrl/anonKey).");

        const { error } = await supabaseClient.storage
            .from(supabaseBucket)
            .upload(path, file, { upsert, contentType });

        if (error) throw new Error(`Supabase 업로드 실패: ${error.message}`);

        const { data } = supabaseClient.storage.from(supabaseBucket).getPublicUrl(path);
        if (!data?.publicUrl) throw new Error("Supabase publicUrl 생성 실패");
        return data.publicUrl;
    }

    async function sbCopy(fromPath, toPath) {
        if (!supabaseClient) throw new Error("Supabase 설정이 없습니다(supabaseUrl/anonKey).");

        const { error } = await supabaseClient.storage
            .from(supabaseBucket)
            .copy(fromPath, toPath);

        if (error) throw new Error(`Supabase copy 실패: ${error.message}`);

        const { data } = supabaseClient.storage.from(supabaseBucket).getPublicUrl(toPath);
        if (!data?.publicUrl) throw new Error("Supabase publicUrl 생성 실패");
        return data.publicUrl;
    }

    async function sbRemove(paths) {
        if (!supabaseClient) return;
        const clean = (paths || []).filter(Boolean);
        if (!clean.length) return;

        const { error } = await supabaseClient.storage.from(supabaseBucket).remove(clean);
        if (error) console.warn("Supabase remove failed:", error.message);
    }

    function buildFinalPath(targetAuctionId, index1, ext) {
        const safeExt = ext || "jpg";
        return `auction_items/${targetAuctionId}/${index1}.${safeExt}`;
    }

    function buildTempPath(tempKey, index1, ext) {
        const safeExt = ext || "jpg";
        return `auction_items/_tmp/${tempKey}/${index1}.${safeExt}`;
    }

    // -------------------------
    // Image pipeline
    // -------------------------
    async function ensureImagesUploadedAndPreparedForDraft() {
        if (!hasPendingFileUploads()) {
            return { uploadedPathsNow: [], finalImageUrls: collectImageUrls(), deferredDeleteOldPaths: [] };
        }

        if (!supabaseClient) {
            throw new Error("파일 업로드를 위해 Supabase 설정이 필요합니다. (supabaseUrl/anonKey)");
        }

        if (!auctionId) {
            // create 모드: temp 업로드는 saveDraft에서 처리
            return { uploadedPathsNow: [], finalImageUrls: collectImageUrls(), deferredDeleteOldPaths: [] };
        }

        const uploadedPathsNow = [];
        const oldSupabasePaths = images
            .map((x) => (x.type === "url" ? x.storagePath : x.storagePath))
            .filter(Boolean);

        const desiredSupabasePaths = [];
        const finalUrls = [];

        const tasks = [];

        images.forEach((img, idx) => {
            const index1 = idx + 1;
            const ext = img.ext || (img.type === "file" ? normalizeExtFromMime(img.file) : extFromUrl(img.url));
            const desiredPath = buildFinalPath(auctionId, index1, ext);

            if (img.type === "file") {
                tasks.push(async () => {
                    const url = await sbUpload(desiredPath, img.file, img.file?.type || "image/jpeg", true);
                    uploadedPathsNow.push(desiredPath);
                    img.url = url;
                    img.storagePath = desiredPath;
                    desiredSupabasePaths.push(desiredPath);
                    finalUrls.push(url);
                });
                return;
            }

            if (img.storagePath) {
                if (img.storagePath !== desiredPath) {
                    tasks.push(async () => {
                        const url = await sbCopy(img.storagePath, desiredPath);
                        uploadedPathsNow.push(desiredPath);
                        img.url = url;
                        img.storagePath = desiredPath;
                        desiredSupabasePaths.push(desiredPath);
                        finalUrls.push(url);
                    });
                } else {
                    desiredSupabasePaths.push(desiredPath);
                    finalUrls.push(img.url);
                }
            } else {
                finalUrls.push(img.url);
            }
        });

        const results = await Promise.allSettled(tasks.map((fn) => fn()));
        const rejected = results.find((r) => r.status === "rejected");

        if (rejected) {
            await sbRemove(uploadedPathsNow);
            throw new Error(rejected.reason?.message || "이미지 업로드 중 일부가 실패했습니다.");
        }

        const desiredExcel = new Set(desiredSupabasePaths);
        const toDeleteAfterSuccess = oldSupabasePaths.filter((p) => p && !RExel.has(p));

        return {
            uploadedPathsNow,
            finalImageUrls: finalUrls,
            deferredDeleteOldPaths: toDeleteAfterSuccess,
        };
    }

    async function createDraftWithTempUploads(payloadWithoutImageUrls) {
        if (!supabaseClient) throw new Error("Supabase 설정이 없습니다(supabaseUrl/anonKey).");

        const tempKey = crypto.randomUUID();
        const uploadedTempPaths = [];
        const tempUrls = [];

        const tasks = [];

        images.forEach((img, idx) => {
            const index1 = idx + 1;
            const ext = img.ext || (img.type === "file" ? normalizeExtFromMime(img.file) : extFromUrl(img.url));

            if (img.type === "file") {
                const tempPath = buildTempPath(tempKey, index1, ext);
                tasks.push(async () => {
                    const url = await sbUpload(tempPath, img.file, img.file?.type || "image/jpeg", true);
                    uploadedTempPaths.push(tempPath);
                    img.url = url;
                    img.storagePath = tempPath;
                    tempUrls[idx] = url;
                });
            } else {
                tempUrls[idx] = img.url;
            }
        });

        const results = await Promise.allSettled(tasks.map((fn) => fn()));
        const rejected = results.find((r) => r.status === "rejected");

        if (rejected) {
            await sbRemove(uploadedTempPaths);
            throw new Error(rejected.reason?.message || "이미지 업로드 중 일부가 실패했습니다.");
        }

        let createdAuctionId = "";
        try {
            const payload = { ...payloadWithoutImageUrls, imageUrls: tempUrls };
            const data = await fetchJson(`${apiBase}/drafts`, { method: "POST", body: payload });
            createdAuctionId = data?.auctionId || "";
            if (!createdAuctionId) throw new Error("draft 생성 응답에 auctionId가 없습니다.");
        } catch (e) {
            await sbRemove(uploadedTempPaths);
            throw e;
        }

        const copiedFinalPaths = [];
        const finalUrls = [];

        try {
            for (let i = 0; i < images.length; i++) {
                const img = images[i];
                const index1 = i + 1;
                const ext = img.ext || extFromUrl(img.url);
                const finalPath = buildFinalPath(createdAuctionId, index1, ext);

                if (img.storagePath && img.storagePath.startsWith("auction_items/_tmp/")) {
                    const url = await sbCopy(img.storagePath, finalPath);
                    copiedFinalPaths.push(finalPath);
                    finalUrls[i] = url;
                    img.url = url;
                    img.storagePath = finalPath;
                } else {
                    finalUrls[i] = img.url;
                }
            }
        } catch (e) {
            await sbRemove(copiedFinalPaths);
            throw e;
        }

        try {
            const updatePayload = { ...payloadWithoutImageUrls, imageUrls: finalUrls };
            await fetchJson(`${apiBase}/drafts/${createdAuctionId}`, { method: "PUT", body: updatePayload });
        } catch (e) {
            await sbRemove(copiedFinalPaths);
            throw e;
        }

        await sbRemove(uploadedTempPaths);
        return createdAuctionId;
    }

    // -------------------------
    // Actions
    // -------------------------
    function hidePublishRetryBox() {
        publishRetryBox?.classList.add("hidden");
    }

    function showPublishRetryBox() {
        publishRetryBox?.classList.remove("hidden");
    }

    async function saveDraft() {
        if (isSaving) return;
        isSaving = true;

        hideAlert();
        clearFieldErrors();
        hidePublishRetryBox();

        try {
            // 기본 검증(이미지 업로드 전)
            const payloadBase = collectPayload([]);
            payloadBase.imageUrls = [];

            const baseErrors = validatePayload({ ...payloadBase, imageUrls: images.length ? ["_"] : [] });
            // 위에서 이미지 최소 1장 조건을 만족시키기 위한 더미 처리, 아래에서 실제 이미지 검증됨
            // -> 사용자 메시지 안정화 목적

            // 실제로 이미지 최소 1장은 반드시
            if (images.length < 1) baseErrors.push(["imageUrls", "이미지를 1장 이상 등록해주세요."]);

            if (baseErrors.length) {
                baseErrors.forEach(([f, m]) => showFieldError(f, m));
                showAlert("필수 입력값을 확인해주세요.");
                return;
            }

            elSaveBtn.disabled = true;
            elSaveBtn.textContent = auctionId ? "저장 중..." : "임시 저장 생성 중...";

            if (!auctionId) {
                if (hasPendingFileUploads() && !supabaseClient) {
                    showAlert("파일 업로드를 위해 Supabase 설정이 필요합니다. (URL로 추가는 가능)");
                    return;
                }

                if (!hasPendingFileUploads()) {
                    const payload = collectPayload(collectImageUrls());
                    const vErrors = validatePayload(payload);
                    if (vErrors.length) {
                        vErrors.forEach(([f, m]) => showFieldError(f, m));
                        showAlert("필수 입력값을 확인해주세요.");
                        return;
                    }

                    const data = await fetchJson(`${apiBase}/drafts`, { method: "POST", body: payload });
                    auctionId = data?.auctionId || "";
                    root.dataset.auctionId = auctionId;
                } else {
                    const payloadBaseNoImages = collectPayload([]);
                    auctionId = await createDraftWithTempUploads(payloadBaseNoImages);
                    root.dataset.auctionId = auctionId;
                }

                setButtonsState();

                const savedAtText = $("#savedAtText");
                const savedAtBadge = $("#savedAtBadge");
                if (savedAtText && savedAtBadge) {
                    const now = new Date();
                    savedAtText.textContent = now.toLocaleString("ko-KR", { hour12: false });
                    savedAtBadge.classList.remove("hidden");
                }

                await loadDraftById(auctionId);

                setToast("저장 완료", "임시 저장이 생성되었습니다.");
                setDirty(false);
                return;
            }

            const { uploadedPathsNow, finalImageUrls, deferredDeleteOldPaths } =
                await ensureImagesUploadedAndPreparedForDraft();

            const payload = collectPayload(finalImageUrls);
            const vErrors = validatePayload(payload);
            if (vErrors.length) {
                vErrors.forEach(([f, m]) => showFieldError(f, m));
                showAlert("필수 입력값을 확인해주세요.");
                return;
            }

            try {
                await fetchJson(`${apiBase}/drafts/${auctionId}`, { method: "PUT", body: payload });
            } catch (e) {
                await sbRemove(uploadedPathsNow);
                throw e;
            }

            await sbRemove(deferredDeleteOldPaths);

            {
                const savedAtText = $("#savedAtText");
                const savedAtBadge = $("#savedAtBadge");
                if (savedAtText && savedAtBadge) {
                    const now = new Date();
                    savedAtText.textContent = now.toLocaleString("ko-KR", { hour12: false });
                    savedAtBadge.classList.remove("hidden");
                }
            }

            await loadDraftById(auctionId);

            setToast("저장 완료", "임시 저장이 완료되었습니다.");
            setDirty(false);
        } catch (e) {
            applyServerFieldErrors(e);
            showAlert(e?.message || "저장에 실패했습니다.");
        } finally {
            elSaveBtn.disabled = false;
            elSaveBtn.textContent = "임시 저장";
            isSaving = false;
            setButtonsState();
        }
    }

    async function publishOnly() {
        if (!auctionId) {
            showAlert("먼저 임시 저장을 완료한 뒤 게시할 수 있습니다.");
            return;
        }

        if (isPublishing) return;
        isPublishing = true;

        hideAlert();
        clearFieldErrors();
        hidePublishRetryBox();

        try {
            elPublishBtn.disabled = true;
            elPublishBtn.textContent = "게시 중...";

            await fetchJson(`${apiBase}/${auctionId}/publish`, { method: "POST" });

            setToast("게시 완료", "경매가 게시되었습니다. 목록으로 이동합니다.");
            window.setTimeout(() => (window.location.href = redirectAfterPublish), 600);
        } catch (e) {
            showPublishRetryBox();
            showAlert(e?.message || "게시에 실패했습니다.");
        } finally {
            elPublishBtn.textContent = "게시하기";
            elPublishBtn.disabled = false;
            isPublishing = false;
        }
    }

    async function publishFlow() {
        if (!auctionId || isDirty || hasPendingFileUploads()) {
            await saveDraft();
        }
        if (!auctionId) return;
        if (isDirty) return;
        if (hasPendingFileUploads()) return;

        await publishOnly();
    }

    async function deleteDraft() {
        hideAlert();
        hidePublishRetryBox();

        if (!auctionId) return;

        const ok = window.confirm("임시 저장된 경매를 삭제하시겠습니까? 삭제 후 복구할 수 없습니다.");
        if (!ok) return;

        try {
            elDeleteBtn.disabled = true;
            elDeleteBtn.textContent = "삭제 중...";

            await fetchJson(`${apiBase}/${auctionId}`, { method: "DELETE" });

            setToast("삭제 완료", "삭제되었습니다. 목록으로 이동합니다.");
            window.setTimeout(() => (window.location.href = redirectAfterDelete), 600);
        } catch (e) {
            showAlert(e?.message || "삭제에 실패했습니다.");
        } finally {
            elDeleteBtn.textContent = "삭제";
            elDeleteBtn.disabled = false;
        }
    }

    // -------------------------
    // Draft load
    // -------------------------
    async function loadDraftById(id) {
        const data = await fetchJson(`${apiBase}/drafts/${id}`, { method: "GET" });

        elTitle.value = data.title ?? "";
        elDesc.value = data.description ?? "";
        elStartPrice.value = data.startPrice != null ? Number(data.startPrice).toLocaleString("ko-KR") : "";
        elDuration.value = data.durationHours != null ? String(data.durationHours) : "";
        elItemName.value = data.itemName ?? "";
        elCategory.value = data.category ?? "";
        elCondition.value = data.condition ?? "";

        // specs 반영: brand/storageGb만
        enableSpecs(Boolean(elCategory.value));
        applySpecsToUI(data.specs || null);

        // images from API
        images.length = 0;
        (data.imageUrls || []).forEach((url) => {
            const storagePath = parseSupabasePathFromPublicUrl(url);
            images.push({
                id: crypto.randomUUID(),
                type: "url",
                url,
                previewUrl: url,
                fileName: url.split("/").pop() || "",
                ext: extFromUrl(url),
                storagePath,
                isExternal: !storagePath,
            });
        });

        renderThumbs();
        updateEndPreview();
        syncSummary();
        setButtonsState();
    }

    async function loadDraftIfNeeded() {
        setButtonsState();
        if (!auctionId) return;

        try {
            await loadDraftById(auctionId);
            setToast("불러오기 완료", "임시 저장 내용을 불러왔습니다.");
            setDirty(false);
        } catch (e) {
            showAlert(e?.message || "임시 저장 내용을 불러오지 못했습니다.");
        }
    }

    // -------------------------
    // Bind actions
    // -------------------------
    function bindActions() {
        elSaveBtn?.addEventListener("click", saveDraft);
        elPublishBtn?.addEventListener("click", publishFlow);
        elDeleteBtn?.addEventListener("click", deleteDraft);

        retryPublishBtn?.addEventListener("click", publishOnly);
    }

    // -------------------------
    // Init
    // -------------------------
    function init() {
        bindCounters();
        bindMoneyInput();
        bindDurationPreview();
        bindSpecs();
        bindImageUpload();
        bindImageUrlModal();
        bindSummary();
        bindActions();

        // dirty tracking
        [elItemName, elCategory, elCondition, elTitle, elDesc, elStartPrice, elDuration].forEach((el) => {
            if (!el) return;
            el.addEventListener("input", () => setDirty(true));
            el.addEventListener("change", () => setDirty(true));
        });

        // 기본 기간(24h)
        applyDefaultDurationIfCreate();

        // initial state
        setButtonsState();
        renderThumbs();
        loadDraftIfNeeded();
    }

    init();
})();
