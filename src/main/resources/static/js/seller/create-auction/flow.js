(() => {
    const ns = (window.EolmagoAuctionDraft = window.EolmagoAuctionDraft || {});
    const { $ } = ns.util;
    const api = ns.api;
    const storage = ns.storage;

    const root = $("#draftPage");
    if (!root) return;

    // -------------------------
    // cfg from dataset
    // -------------------------
    const cfg = {
        apiBase: (root.dataset.apiBase || "/api/auctions").trim(),
        auctionId: (root.dataset.auctionId || "").trim(),
        mode: (root.dataset.mode || "create").trim(),

        redirectAfterPublish: (root.dataset.redirectAfterPublish || "/seller/auctions").trim(),
        redirectAfterDelete: (root.dataset.redirectAfterDelete || "/seller/auctions").trim(),

        supabaseUrl: (root.dataset.supabaseUrl || "").trim(),
        supabaseAnonKey: (root.dataset.supabaseAnonKey || "").trim(),
        supabaseBucket: (root.dataset.supabaseBucket || "eolmago").trim(),
        supabaseBasePath: (root.dataset.supabaseBasePath || "auction_items").trim(),
    };

    ns.cfg = cfg;

    // -------------------------
    // UI
    // -------------------------
    const ui = ns.ui.createUI(root);

    function setButtonsState() {
        // 삭제만 auctionId 있을 때 활성
        ui.el.deleteBtn.disabled = !cfg.auctionId;

        // 게시하기는 "처음 글이어도 가능"이므로 기본적으로 disabled 하지 않음
        // 다만 저장/게시 중에는 클릭 방지
        if (ui.state.isSaving || ui.state.isPublishing) {
            ui.el.publishBtn.disabled = true;
            ui.el.saveBtn.disabled = true;
            ui.el.deleteBtn.disabled = true;
        } else {
            ui.el.publishBtn.disabled = false;
            ui.el.saveBtn.disabled = false;
            ui.el.deleteBtn.disabled = !cfg.auctionId;
        }

        if (ui.el.statusText) ui.el.statusText.textContent = cfg.auctionId ? "임시 저장" : "작성 중";
    }

    function syncAuctionId(newId) {
        cfg.auctionId = newId || "";
        root.dataset.auctionId = cfg.auctionId;
        setButtonsState();
    }

    // -------------------------
    // Load draft if exists
    // -------------------------
    async function loadDraftIfNeeded() {
        setButtonsState();
        if (!cfg.auctionId) return;

        try {
            const data = await api.getDraft(cfg.apiBase, cfg.auctionId);
            ui.loadDraftToUI(data, storage.parsePathFromPublicUrl);
            ui.setToast("불러오기 완료", "임시 저장 내용을 불러왔습니다.");
        } catch (e) {
            ui.showAlert(e?.message || "임시 저장 내용을 불러오지 못했습니다.");
        }
    }

    // -------------------------
    // Save draft
    // -------------------------
    async function saveDraft() {
        if (ui.state.isSaving) return;
        ui.state.isSaving = true;

        ui.hideAlert();
        ui.clearFieldErrors();
        ui.hidePublishRetryBox();

        try {
            setButtonsState();
            ui.el.saveBtn.textContent = cfg.auctionId ? "저장 중..." : "임시 저장 생성 중...";

            // 1) 기본 검증(이미지 포함)
            // create에서 file은 아직 url이 없으므로, 검증용으로 dummy 처리
            const basePayload = ui.collectPayload([]);
            const dummyUrls = ui.state.images.length ? ["_dummy_"] : [];
            const baseErrors = ui.validatePayload({ ...basePayload, imageUrls: dummyUrls });

            if (ui.state.images.length < 1) baseErrors.push(["imageUrls", "이미지를 1장 이상 등록해주세요."]);

            if (baseErrors.length) {
                baseErrors.forEach(([f, m]) => ui.showFieldError(f, m));
                ui.showAlert("필수 입력값을 확인해주세요.");
                return;
            }

            // 2) create(auctionId 없음)
            if (!cfg.auctionId) {
                // 파일이 있다면: (이미지 업로드 -> createDraft -> final move -> draft update)
                const hasFile = ui.state.images.some((x) => x.type === "file");
                if (hasFile) {
                    const payloadNoImages = ui.collectPayload([]);
                    const newId = await storage.createDraftWithTempUploads({
                        apiBase: cfg.apiBase,
                        payloadWithoutImageUrls: payloadNoImages,
                        images: ui.state.images,
                        createDraftFn: api.createDraft,
                        updateDraftFn: api.updateDraft,
                    });
                    syncAuctionId(newId);
                } else {
                    // 파일이 없고(사실상 현재 UI에서는 거의 없음), 이미 url만 있다면 그대로 create
                    const payload = ui.collectPayload(ui.collectImageUrls());
                    const data = await api.createDraft(cfg.apiBase, payload);
                    const newId = data?.auctionId || "";
                    if (!newId) throw new Error("draft 생성 응답에 auctionId가 없습니다.");
                    syncAuctionId(newId);
                }

                ui.setSavedAtNow();

                // 서버 기준으로 다시 로드(정합성)
                const loaded = await api.getDraft(cfg.apiBase, cfg.auctionId);
                ui.loadDraftToUI(loaded, storage.parsePathFromPublicUrl);

                ui.setToast("저장 완료", "임시 저장이 생성되었습니다.");
                ui.setDirty(false);
                return;
            }

            // 3) update(auctionId 있음)
            // 기존 draft는 “최종 경로 정렬 + 파일 업로드 + 삭제된 이미지 정리”를 먼저 수행
            const { finalImageUrls } = await storage.ensureFinalImageUrlsForExistingDraft({
                auctionId: cfg.auctionId,
                images: ui.state.images,
                loadedSupabasePaths: ui.state.loadedSupabasePaths,
            });

            const payload = ui.collectPayload(finalImageUrls);
            const vErrors = ui.validatePayload(payload);
            if (vErrors.length) {
                vErrors.forEach(([f, m]) => ui.showFieldError(f, m));
                ui.showAlert("필수 입력값을 확인해주세요.");
                return;
            }

            await api.updateDraft(cfg.apiBase, cfg.auctionId, payload);

            ui.setSavedAtNow();

            const loaded = await api.getDraft(cfg.apiBase, cfg.auctionId);
            ui.loadDraftToUI(loaded, storage.parsePathFromPublicUrl);

            ui.setToast("저장 완료", "임시 저장이 완료되었습니다.");
            ui.setDirty(false);
        } catch (e) {
            api.applyServerFieldErrors(e, ui.showFieldError);
            ui.showAlert(e?.message || "저장에 실패했습니다.");
        } finally {
            ui.el.saveBtn.textContent = "임시 저장";
            ui.state.isSaving = false;
            setButtonsState();
        }
    }

    // -------------------------
    // Publish
    // -------------------------
    async function publishOnly() {
        if (!cfg.auctionId) {
            ui.showAlert("게시 전에 경매 ID가 필요합니다. 자동 저장 후 다시 시도해주세요.");
            return;
        }

        if (ui.state.isPublishing) return;
        ui.state.isPublishing = true;

        ui.hideAlert();
        ui.clearFieldErrors();
        ui.hidePublishRetryBox();

        try {
            setButtonsState();
            ui.el.publishBtn.textContent = "게시 중...";

            await api.publish(cfg.apiBase, cfg.auctionId);

            ui.setToast("게시 완료", "경매가 게시되었습니다. 목록으로 이동합니다.");
            window.setTimeout(() => (window.location.href = cfg.redirectAfterPublish), 600);
        } catch (e) {
            ui.showPublishRetryBox();
            ui.showAlert(e?.message || "게시에 실패했습니다.");
        } finally {
            ui.el.publishBtn.textContent = "게시하기";
            ui.state.isPublishing = false;
            setButtonsState();
        }
    }

    /**
     * 요구사항(명령3) 반영:
     * - 임시 저장이 없어서 처음 작성이어도 "게시하기" 가능
     * - 흐름: (이미지 업로드 포함) saveDraft()로 auctionId 생성/업데이트 → publishOnly()
     */
    async function publishFlow() {
        // draft가 없거나, 수정 중이거나, 파일이 남아있으면 저장을 먼저 수행
        const needSave = !cfg.auctionId || ui.state.isDirty || ui.hasPendingFileUploads();
        if (needSave) await saveDraft();

        // 저장 실패/검증 실패면 여기서 중단
        if (!cfg.auctionId) return;
        if (ui.state.isDirty) return;
        if (ui.hasPendingFileUploads()) return;

        await publishOnly();
    }

    // -------------------------
    // Delete
    // -------------------------
    async function deleteDraft() {
        ui.hideAlert();
        ui.hidePublishRetryBox();

        if (!cfg.auctionId) return;

        const ok = window.confirm("임시 저장된 경매를 삭제하시겠습니까? 삭제 후 복구할 수 없습니다.");
        if (!ok) return;

        try {
            ui.state.isSaving = true;
            setButtonsState();

            ui.el.deleteBtn.textContent = "삭제 중...";
            await api.deleteAuction(cfg.apiBase, cfg.auctionId);

            ui.setToast("삭제 완료", "삭제되었습니다. 목록으로 이동합니다.");
            window.setTimeout(() => (window.location.href = cfg.redirectAfterDelete), 600);
        } catch (e) {
            ui.showAlert(e?.message || "삭제에 실패했습니다.");
        } finally {
            ui.el.deleteBtn.textContent = "삭제";
            ui.state.isSaving = false;
            setButtonsState();
        }
    }

    // -------------------------
    // Bind actions
    // -------------------------
    ui.el.saveBtn?.addEventListener("click", saveDraft);
    ui.el.publishBtn?.addEventListener("click", publishFlow);
    ui.el.deleteBtn?.addEventListener("click", deleteDraft);
    ui.el.retryPublishBtn?.addEventListener("click", publishOnly);

    // init
    setButtonsState();
    loadDraftIfNeeded();
})();
