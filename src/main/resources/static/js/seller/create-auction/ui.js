(() => {
    const ns = (window.EolmagoAuctionDraft = window.EolmagoAuctionDraft || {});
    const {
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
    } = ns.util;

    const MAX_IMAGES = 10;
    const DND_TYPE = "application/x-eolmago-image-index";

    function createUI(root) {
        // -------------------------
        // Elements
        // -------------------------
        const el = {
            root,

            // fields
            itemName: $("#itemName", root),
            category: $("#category", root),
            condition: $("#condition", root),
            title: $("#title", root),
            desc: $("#description", root),
            startPrice: $("#startPrice", root),
            duration: $("#durationHours", root),

            // specs
            brandSelect: $("#brandSelect", root),
            brandCustomWrap: $("#brandCustomWrap", root),
            brandCustomInput: $("#brandCustomInput", root),
            storageGb: $("#storageGb", root),

            // previews
            endAtPreview: $("#endAtPreview", root),

            // alert
            alert: $("#formAlert", root),

            // summary
            summaryItemName: $("#summaryItemName", root),
            summaryStartPrice: $("#summaryStartPrice", root),
            summaryDuration: $("#summaryDuration", root),
            summaryImages: $("#summaryImages", root),

            // counters
            titleCount: $("#titleCount", root),
            descCount: $("#descCount", root),

            // images
            imageInput: $("#imageInput", root),
            dropzone: $("#dropzone", root),
            thumbGrid: $("#thumbGrid", root),
            imageCount: $("#imageCount", root),

            // buttons
            saveBtn: $("#saveDraftBtn", root),
            publishBtn: $("#publishBtn", root),
            deleteBtn: $("#deleteBtn", root),

            // status
            statusText: $("#statusText", root),
            savedAtText: $("#savedAtText", root),
            savedAtBadge: $("#savedAtBadge", root),

            // toast
            toast: $("#toast", root),
            toastTitle: $("#toastTitle", root),
            toastMsg: $("#toastMsg", root),

            // retry box
            publishRetryBox: $("#publishRetryBox", root),
            retryPublishBtn: $("#retryPublishBtn", root),
        };

        // -------------------------
        // State
        // -------------------------
        const state = {
            images: [], // {id, type:'file'|'url', file?, previewUrl, url, storagePath, ext}
            isDirty: false,
            isSaving: false,
            isPublishing: false,

            // draft 로드 시점에 서버에 존재하던 supabase path들(삭제 판단용)
            loadedSupabasePaths: new Set(),
        };

        // -------------------------
        // UI helpers
        // -------------------------
        function setDirty(v = true) {
            state.isDirty = v;
        }

        function showAlert(msg) {
            el.alert.textContent = msg;
            el.alert.classList.remove("hidden");
            el.alert.scrollIntoView({ behavior: "smooth", block: "start" });
        }

        function hideAlert() {
            el.alert.classList.add("hidden");
            el.alert.textContent = "";
        }

        function showFieldError(name, msg) {
            const target = document.querySelector(`[data-error="${name}"]`);
            if (!target) return;
            target.textContent = msg;
            target.classList.remove("hidden");
        }

        function clearFieldErrors() {
            $$(`[data-error]`).forEach((x) => {
                x.textContent = "";
                x.classList.add("hidden");
            });
        }

        function setToast(title, msg) {
            if (!el.toast) return;
            el.toastTitle.textContent = title;
            el.toastMsg.textContent = msg;
            el.toast.classList.remove("hidden");
            window.clearTimeout(setToast._t);
            setToast._t = window.setTimeout(() => el.toast.classList.add("hidden"), 2600);
        }

        function ensureImageCapacity(countToAdd) {
            if (state.images.length + countToAdd > MAX_IMAGES) {
                setToast("이미지 제한", `최대 ${MAX_IMAGES}장까지 등록할 수 있습니다.`);
                return false;
            }
            return true;
        }

        function updateImageCount() {
            if (el.imageCount) el.imageCount.textContent = String(state.images.length);
            if (el.summaryImages) el.summaryImages.textContent = String(state.images.length);
        }

        function syncSummary() {
            if (el.summaryItemName) el.summaryItemName.textContent = (el.itemName?.value || "").trim() || "-";
            if (el.summaryStartPrice) el.summaryStartPrice.textContent = fmtMoney(el.startPrice?.value || "");
            if (el.summaryDuration) el.summaryDuration.textContent = durationLabel(el.duration?.value);
            if (el.summaryImages) el.summaryImages.textContent = String(state.images.length);
        }

        function updateCounters() {
            if (el.titleCount) el.titleCount.textContent = String(el.title?.value?.length || 0);
            if (el.descCount) el.descCount.textContent = String(el.desc?.value?.length || 0);
        }

        function updateEndPreview() {
            if (!el.endAtPreview) return;
            const hours = Number(el.duration?.value || "");
            if (!hours || !Number.isFinite(hours)) {
                el.endAtPreview.textContent = "기간을 선택하면 표시됩니다";
                return;
            }
            const now = new Date();
            const end = new Date(now.getTime() + hours * 60 * 60 * 1000);
            el.endAtPreview.textContent = `${formatKoreanDate(end)} 종료`;
        }

        // -------------------------
        // Specs (brand + storageGb)
        // -------------------------
        function enableSpecs(enabled) {
            if (el.brandSelect) el.brandSelect.disabled = !enabled;
            if (el.storageGb) el.storageGb.disabled = !enabled;

            if (!enabled) {
                el.brandCustomWrap?.classList.add("hidden");
                if (el.brandCustomInput) el.brandCustomInput.value = "";
                if (el.brandSelect) el.brandSelect.value = "";
                if (el.storageGb) el.storageGb.value = "";
            }
        }

        function collectSpecs() {
            const cat = (el.category?.value || "").trim();
            if (!cat) return null;

            const needs = ["PHONE", "TABLET"].includes(cat);
            if (!needs) return null;

            let brand = (el.brandSelect?.value || "").trim();
            if (brand === "__custom__") {
                brand = (el.brandCustomInput?.value || "").trim();
            }
            const storageGb = numericOnly(el.storageGb?.value);

            return { brand: brand || null, storageGb };
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
            const brand = (specs?.brand ?? "").toString().trim();
            const storage = specs?.storageGb;

            enableSpecs(Boolean(el.category?.value));

            const known = ["Apple", "Samsung"];
            if (brand && known.includes(brand)) {
                el.brandSelect.value = brand;
                el.brandCustomWrap.classList.add("hidden");
                el.brandCustomInput.value = "";
            } else if (brand) {
                el.brandSelect.value = "__custom__";
                el.brandCustomWrap.classList.remove("hidden");
                el.brandCustomInput.value = brand;
            } else {
                el.brandSelect.value = "";
                el.brandCustomWrap.classList.add("hidden");
                el.brandCustomInput.value = "";
            }

            el.storageGb.value = storage != null ? String(storage) : "";
        }

        // -------------------------
        // Images UI (업로드 대기 라벨 제거 반영)
        // -------------------------
        function renderThumbs() {
            if (!el.thumbGrid) return;
            el.thumbGrid.innerHTML = "";

            if (state.images.length === 0) {
                const empty = document.createElement("div");
                empty.className =
                    "col-span-full rounded-xl border border-gray-200 bg-white px-4 py-4 text-sm text-gray-600";
                empty.textContent = "아직 등록된 이미지가 없습니다. 최소 1장을 등록해주세요.";
                el.thumbGrid.appendChild(empty);
                updateImageCount();
                return;
            }

            state.images.forEach((img, idx) => {
                const card = document.createElement("div");
                card.className = "group relative overflow-hidden rounded-2xl border border-gray-200 bg-white";
                card.draggable = true;
                card.dataset.index = String(idx);

                const src = img.previewUrl || img.url || "";

                card.innerHTML = `
          <div class="relative aspect-square bg-gray-100">
            ${
                    src
                        ? `<img src="${escapeHtml(src)}" alt="미리보기" class="h-full w-full object-cover select-none pointer-events-none" />`
                        : `<div class="h-full w-full flex items-center justify-center text-sm text-gray-500">미리보기 없음</div>`
                }

            ${
                    idx === 0
                        ? `<span class="absolute left-2 top-2 inline-flex items-center rounded-full bg-slate-900/90 px-2.5 py-1 text-[11px] font-semibold text-white">
                     대표
                   </span>`
                        : ``
                }

            <button type="button"
                    class="absolute right-2 top-2 inline-flex h-8 w-8 items-center justify-center rounded-full bg-red-600 text-white shadow-sm hover:bg-red-700"
                    data-remove
                    aria-label="이미지 제거">
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                <path d="M18 6 6 18"></path>
                <path d="m6 6 12 12"></path>
              </svg>
            </button>
          </div>
        `;

                card.querySelector("[data-remove]").addEventListener("click", (e) => {
                    e.preventDefault();
                    e.stopPropagation();

                    if (img.type === "file" && img.previewUrl) {
                        try { URL.revokeObjectURL(img.previewUrl); } catch {}
                    }
                    state.images.splice(idx, 1);
                    renderThumbs();
                    syncSummary();
                    setDirty(true);
                });

                // Drag reorder
                card.addEventListener("dragstart", (e) => {
                    e.dataTransfer.effectAllowed = "move";
                    e.dataTransfer.setData(DND_TYPE, String(idx));
                    e.dataTransfer.setData("text/plain", "");
                    card.classList.add("opacity-60");
                });

                card.addEventListener("dragend", () => {
                    card.classList.remove("opacity-60");
                    card.classList.remove("ring-4", "ring-slate-900/10");
                });

                card.addEventListener("dragover", (e) => {
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
                    e.stopPropagation();
                    card.classList.remove("ring-4", "ring-slate-900/10");

                    const from = Number(e.dataTransfer.getData(DND_TYPE));
                    const to = idx;
                    if (!Number.isFinite(from) || from < 0 || from >= state.images.length) return;
                    if (from === to) return;

                    const moved = state.images.splice(from, 1)[0];
                    state.images.splice(to, 0, moved);

                    renderThumbs();
                    syncSummary();
                    setDirty(true);
                });

                el.thumbGrid.appendChild(card);
            });

            updateImageCount();
        }

        function bindImageUpload() {
            el.imageInput?.addEventListener("change", () => {
                const files = Array.from(el.imageInput.files || []);
                if (!files.length) return;
                if (!ensureImageCapacity(files.length)) return;

                files.forEach((f) => {
                    state.images.push({
                        id: uuid(),
                        type: "file",
                        file: f,
                        ext: normalizeExtFromMime(f),
                        previewUrl: URL.createObjectURL(f),
                        url: "",
                        storagePath: null,
                    });
                });

                renderThumbs();
                syncSummary();
                setDirty(true);
                el.imageInput.value = "";
            });

            if (!el.dropzone) return;

            ["dragenter", "dragover"].forEach((evt) =>
                el.dropzone.addEventListener(evt, (e) => {
                    if (e.dataTransfer?.types?.includes(DND_TYPE)) return;
                    e.preventDefault();
                    el.dropzone.classList.add("border-slate-900", "bg-white");
                })
            );

            ["dragleave", "drop"].forEach((evt) =>
                el.dropzone.addEventListener(evt, (e) => {
                    if (e.dataTransfer?.types?.includes(DND_TYPE)) return;
                    e.preventDefault();
                    el.dropzone.classList.remove("border-slate-900", "bg-white");
                })
            );

            el.dropzone.addEventListener("drop", (e) => {
                if (e.dataTransfer?.types?.includes(DND_TYPE)) return;

                const dt = e.dataTransfer;
                if (!dt?.files?.length) return;

                const files = Array.from(dt.files);
                if (!ensureImageCapacity(files.length)) return;

                files.forEach((f) => {
                    state.images.push({
                        id: uuid(),
                        type: "file",
                        file: f,
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

        function collectImageUrls() {
            return state.images.map((x) => x.url).filter(Boolean);
        }

        function hasPendingFileUploads() {
            return state.images.some((x) => x.type === "file" && !x.url);
        }

        // -------------------------
        // Payload + validation
        // -------------------------
        function collectPayload(imageUrlsOverride) {
            const specs = collectSpecs();
            return {
                title: (el.title?.value || "").trim(),
                description: (el.desc?.value || "").trim() || null,
                startPrice: numericValue(el.startPrice?.value),
                durationHours: el.duration?.value ? Number(el.duration.value) : null,
                itemName: (el.itemName?.value || "").trim(),
                category: el.category?.value || null,
                condition: el.condition?.value || null,
                specs,
                imageUrls: imageUrlsOverride ?? collectImageUrls(),
            };
        }

        function validatePayload(payload) {
            const errors = [];

            if (!payload.title?.trim()) errors.push(["title", "제목을 입력해주세요."]);
            if (!payload.itemName?.trim()) errors.push(["itemName", "상품명을 입력해주세요."]);
            if (!payload.category) errors.push(["category", "카테고리를 선택해주세요."]);
            if (!payload.condition) errors.push(["condition", "상품 상태를 선택해주세요."]);

            errors.push(...validateSpecs(payload));

            if (payload.startPrice == null) errors.push(["startPrice", "시작가를 입력해주세요."]);
            if (payload.startPrice != null && (payload.startPrice < 10000 || payload.startPrice > 10000000)) {
                errors.push(["startPrice", "시작가는 10,000원 이상 10,000,000원 이하여야 합니다."]);
            }

            if (payload.durationHours == null) errors.push(["durationHours", "경매 기간을 선택해주세요."]);
            if (payload.durationHours != null && (payload.durationHours < 12 || payload.durationHours > 168)) {
                errors.push(["durationHours", "경매 기간은 12~168시간 사이여야 합니다."]);
            }

            if (!payload.imageUrls || payload.imageUrls.length < 1) errors.push(["imageUrls", "이미지를 1장 이상 등록해주세요."]);
            if (payload.imageUrls && payload.imageUrls.length > 10) errors.push(["imageUrls", "이미지는 최대 10장까지 등록할 수 있습니다."]);

            return errors;
        }

        // -------------------------
        // Draft load (API data -> UI)
        // -------------------------
        function loadDraftToUI(data, parsePathFn) {
            el.title.value = data.title ?? "";
            el.desc.value = data.description ?? "";
            el.startPrice.value = data.startPrice != null ? Number(data.startPrice).toLocaleString("ko-KR") : "";
            el.duration.value = data.durationHours != null ? String(data.durationHours) : "";
            el.itemName.value = data.itemName ?? "";
            el.category.value = data.category ?? "";
            el.condition.value = data.condition ?? "";

            enableSpecs(Boolean(el.category.value));
            applySpecsToUI(data.specs || null);

            state.images.length = 0;
            state.loadedSupabasePaths = new Set();

            (data.imageUrls || []).forEach((url) => {
                const storagePath = parsePathFn ? parsePathFn(url) : null;
                if (storagePath) state.loadedSupabasePaths.add(storagePath);

                state.images.push({
                    id: uuid(),
                    type: "url",
                    url,
                    previewUrl: url,
                    ext: extFromUrl(url),
                    storagePath: storagePath,
                });
            });

            renderThumbs();
            updateCounters();
            updateEndPreview();
            syncSummary();
            setDirty(false);
        }

        // -------------------------
        // Bindings
        // -------------------------
        function bindBasics() {
            [el.title, el.desc].forEach((x) => x?.addEventListener("input", updateCounters));
            updateCounters();

            el.startPrice?.addEventListener("input", () => {
                const raw = el.startPrice.value.replace(/[^\d]/g, "");
                el.startPrice.value = raw ? Number(raw).toLocaleString("ko-KR") : "";
                syncSummary();
                setDirty(true);
            });

            el.duration?.addEventListener("change", () => {
                updateEndPreview();
                syncSummary();
                setDirty(true);
            });

            // create 최초 진입 기본 기간 24시간
            if (!el.duration.value) {
                el.duration.value = "24";
                updateEndPreview();
                syncSummary();
            }

            // specs
            enableSpecs(Boolean(el.category?.value));

            el.category?.addEventListener("change", () => {
                enableSpecs(Boolean(el.category.value));
                setDirty(true);
            });

            el.brandSelect?.addEventListener("change", () => {
                const v = el.brandSelect.value;
                if (v === "__custom__") {
                    el.brandCustomWrap.classList.remove("hidden");
                    el.brandCustomInput.focus();
                } else {
                    el.brandCustomWrap.classList.add("hidden");
                    el.brandCustomInput.value = "";
                }
                setDirty(true);
            });

            el.storageGb?.addEventListener("input", () => {
                el.storageGb.value = String(el.storageGb.value || "").replace(/[^\d]/g, "");
                setDirty(true);
            });

            el.brandCustomInput?.addEventListener("input", () => setDirty(true));

            // dirty tracking
            [el.itemName, el.category, el.condition, el.title, el.desc, el.startPrice, el.duration].forEach((x) => {
                if (!x) return;
                x.addEventListener("input", () => { syncSummary(); setDirty(true); });
                x.addEventListener("change", () => { syncSummary(); setDirty(true); });
            });

            syncSummary();
        }

        function setSavedAtNow() {
            if (el.savedAtText && el.savedAtBadge) {
                const now = new Date();
                el.savedAtText.textContent = now.toLocaleString("ko-KR", { hour12: false });
                el.savedAtBadge.classList.remove("hidden");
            }
        }

        function hidePublishRetryBox() {
            el.publishRetryBox?.classList.add("hidden");
        }

        function showPublishRetryBox() {
            el.publishRetryBox?.classList.remove("hidden");
        }

        // init
        bindBasics();
        bindImageUpload();
        renderThumbs();

        return {
            el,
            state,
            setDirty,
            showAlert,
            hideAlert,
            showFieldError,
            clearFieldErrors,
            setToast,
            syncSummary,
            updateEndPreview,
            collectPayload,
            validatePayload,
            collectImageUrls,
            hasPendingFileUploads,
            enableSpecs,
            applySpecsToUI,
            loadDraftToUI,
            setSavedAtNow,
            hidePublishRetryBox,
            showPublishRetryBox,
        };
    }

    ns.ui = { createUI };
})();
