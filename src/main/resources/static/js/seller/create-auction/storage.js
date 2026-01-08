(() => {
    const ns = (window.EolmagoAuctionDraft = window.EolmagoAuctionDraft || {});
    const { extFromUrl, normalizeExtFromMime } = ns.util || {};

    function getCfg() {
        return ns.cfg || {};
    }

    function getClient() {
        const cfg = getCfg();
        if (ns._sbClient) return ns._sbClient;

        const supabaseUrl = (cfg.supabaseUrl || "").trim();
        const supabaseAnonKey = (cfg.supabaseAnonKey || "").trim();
        if (!supabaseUrl || !supabaseAnonKey) return null;

        const lib = window.supabase;
        if (!lib || typeof lib.createClient !== "function") return null;

        ns._sbClient = lib.createClient(supabaseUrl, supabaseAnonKey);
        return ns._sbClient;
    }

    function publicPrefix() {
        const cfg = getCfg();
        const sb = getClient();
        if (!sb) return "";
        const base = (cfg.supabaseUrl || "").replace(/\/$/, "");
        return `${base}/storage/v1/object/public/${cfg.supabaseBucket}/`;
    }

    function parsePathFromPublicUrl(url) {
        const prefix = publicPrefix();
        if (!prefix) return null;
        if (!url?.startsWith(prefix)) return null;
        return url.substring(prefix.length);
    }

    async function sbUpload(bucket, path, file, contentType, upsert = true) {
        const sb = getClient();
        if (!sb) throw new Error("Supabase 설정이 없습니다(supabaseUrl/anonKey).");

        const { error } = await sb.storage.from(bucket).upload(path, file, { upsert, contentType });
        if (error) throw new Error(`Supabase 업로드 실패: ${error.message}`);

        const { data } = sb.storage.from(bucket).getPublicUrl(path);
        if (!data?.publicUrl) throw new Error("Supabase publicUrl 생성 실패");
        return data.publicUrl;
    }

    async function sbMove(bucket, fromPath, toPath) {
        const sb = getClient();
        if (!sb) throw new Error("Supabase 설정이 없습니다(supabaseUrl/anonKey).");

        const { error } = await sb.storage.from(bucket).move(fromPath, toPath);
        if (error) throw new Error(`Supabase move 실패: ${error.message}`);

        const { data } = sb.storage.from(bucket).getPublicUrl(toPath);
        if (!data?.publicUrl) throw new Error("Supabase publicUrl 생성 실패");
        return data.publicUrl;
    }

    async function sbCopy(bucket, fromPath, toPath) {
        const sb = getClient();
        if (!sb) throw new Error("Supabase 설정이 없습니다(supabaseUrl/anonKey).");

        const { error } = await sb.storage.from(bucket).copy(fromPath, toPath);
        if (error) throw new Error(`Supabase copy 실패: ${error.message}`);

        const { data } = sb.storage.from(bucket).getPublicUrl(toPath);
        if (!data?.publicUrl) throw new Error("Supabase publicUrl 생성 실패");
        return data.publicUrl;
    }

    async function sbRemove(bucket, paths) {
        const sb = getClient();
        if (!sb) return;

        const clean = (paths || []).filter(Boolean);
        if (!clean.length) return;

        const { error } = await sb.storage.from(bucket).remove(clean);
        if (error) console.warn("Supabase remove failed:", error.message);
    }

    function buildFinalPath(basePath, auctionId, index1, ext) {
        const safeExt = ext || "jpg";
        return `${basePath}/${auctionId}/${index1}.${safeExt}`;
    }

    function buildTempPath(basePath, tempKey, index1, ext) {
        const safeExt = ext || "jpg";
        return `${basePath}/_tmp/${tempKey}/${index1}.${safeExt}`;
    }

    /**
     * 기존 draft(auctionId 존재) 저장 시:
     * - 새 파일이 있거나 순서가 바뀌면 staging 경유로 정렬(move/upload -> remove(final) -> move)
     * - 삭제된 이전 이미지(loadedSupabasePaths - referencedPaths)는 remove 시도
     */
    async function ensureFinalImageUrlsForExistingDraft({ auctionId, images, loadedSupabasePaths }) {
        const cfg = getCfg();
        const bucket = cfg.supabaseBucket;
        const basePath = cfg.supabaseBasePath || "auction_items";

        const pendingFiles = images.some((x) => x.type === "file" && !x.url);

        const referencedPaths = new Set(
            images.map((x) => x.storagePath).filter((p) => typeof p === "string" && p.length > 0)
        );
        const toDelete = Array.from(loadedSupabasePaths || []).filter((p) => p && !referencedPaths.has(p));

        // 새 파일이 없으면: 현재 URL 그대로 + 삭제된 것 remove만 시도
        if (!pendingFiles) {
            await sbRemove(bucket, toDelete);
            return {
                finalImageUrls: images.map((x) => x.url).filter(Boolean),
                deletedPaths: toDelete,
            };
        }

        const sb = getClient();
        if (!sb) throw new Error("파일 업로드를 위해 Supabase 설정이 필요합니다. (supabaseUrl/anonKey)");

        const stagingKey = (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : `${Date.now()}`;
        const staged = []; // { idx, stagePath, finalPath }

        // 1) staging으로 이동/업로드
        for (let i = 0; i < images.length; i++) {
            const img = images[i];
            const index1 = i + 1;

            const ext =
                img.ext ||
                (img.type === "file" ? normalizeExtFromMime(img.file) : extFromUrl(img.url));

            const stagePath = `${basePath}/${auctionId}/_staging/${stagingKey}/${index1}.${ext}`;
            const finalPath = buildFinalPath(basePath, auctionId, index1, ext);

            if (img.type === "file") {
                const url = await sbUpload(bucket, stagePath, img.file, img.file?.type || "image/jpeg", true);
                img.url = url;
                img.storagePath = stagePath;
                img.ext = ext;
                staged.push({ idx: i, stagePath, finalPath });
                continue;
            }

            if (img.storagePath) {
                const url = await sbMove(bucket, img.storagePath, stagePath);
                img.url = url;
                img.storagePath = stagePath;
                img.ext = ext;
                staged.push({ idx: i, stagePath, finalPath });
                continue;
            }

            // 외부 URL은 그대로 유지
        }

        // 2) finalPath 및 삭제대상 remove (충돌 방지)
        await sbRemove(bucket, [
            ...staged.map((x) => x.finalPath),
            ...toDelete,
        ]);

        // 3) staging -> final move
        const finalUrls = new Array(images.length);
        for (const s of staged) {
            const url = await sbMove(bucket, s.stagePath, s.finalPath);
            images[s.idx].url = url;
            images[s.idx].storagePath = s.finalPath;
            finalUrls[s.idx] = url;
        }

        // 4) 외부 URL 보정
        for (let i = 0; i < images.length; i++) {
            if (!finalUrls[i] && images[i].url) finalUrls[i] = images[i].url;
        }

        return { finalImageUrls: finalUrls.filter(Boolean), deletedPaths: toDelete };
    }

    /**
     * create 모드 게시/저장 흐름:
     * 1) 파일은 _tmp 업로드
     * 2) createDraft(임시 URL로)
     * 3) _tmp -> final(move)로 정리
     * 4) draft PUT로 최종 URL 반영
     */
    async function createDraftWithTempUploads({
                                                  apiBase,
                                                  payloadWithoutImageUrls,
                                                  images,
                                                  createDraftFn,
                                                  updateDraftFn,
                                              }) {
        const cfg = getCfg();
        const bucket = cfg.supabaseBucket;
        const basePath = cfg.supabaseBasePath || "auction_items";

        const sb = getClient();
        if (!sb) throw new Error("Supabase 설정이 없습니다(supabaseUrl/anonKey).");

        const tempKey = (window.crypto && crypto.randomUUID) ? crypto.randomUUID() : `${Date.now()}`;
        const uploadedTempPaths = [];
        const tempUrls = new Array(images.length);

        // 1) temp 업로드
        for (let i = 0; i < images.length; i++) {
            const img = images[i];
            const index1 = i + 1;

            const ext =
                img.ext ||
                (img.type === "file" ? normalizeExtFromMime(img.file) : extFromUrl(img.url));

            if (img.type === "file") {
                const tempPath = buildTempPath(basePath, tempKey, index1, ext);
                const url = await sbUpload(bucket, tempPath, img.file, img.file?.type || "image/jpeg", true);
                uploadedTempPaths.push(tempPath);
                img.url = url;
                img.storagePath = tempPath;
                img.ext = ext;
                tempUrls[i] = url;
            } else {
                tempUrls[i] = img.url;
            }
        }

        // 2) draft 생성
        let createdAuctionId = "";
        try {
            const payload = { ...payloadWithoutImageUrls, imageUrls: tempUrls.filter(Boolean) };
            const data = await createDraftFn(apiBase, payload);
            createdAuctionId = data?.auctionId || "";
            if (!createdAuctionId) throw new Error("draft 생성 응답에 auctionId가 없습니다.");
        } catch (e) {
            await sbRemove(bucket, uploadedTempPaths);
            throw e;
        }

        // 3) temp -> final move
        const finalUrls = new Array(images.length);
        const movedFinalPaths = [];

        try {
            for (let i = 0; i < images.length; i++) {
                const img = images[i];
                const index1 = i + 1;
                const ext = img.ext || extFromUrl(img.url);
                const finalPath = buildFinalPath(basePath, createdAuctionId, index1, ext);

                if (img.storagePath && img.storagePath.includes(`/${basePath}/_tmp/`) === false && img.storagePath.startsWith(`${basePath}/_tmp/`) === false) {
                    // 혹시 다른 경로면 그대로
                }

                if (img.storagePath && img.storagePath.startsWith(`${basePath}/_tmp/`)) {
                    const url = await sbMove(bucket, img.storagePath, finalPath);
                    movedFinalPaths.push(finalPath);
                    finalUrls[i] = url;
                    img.url = url;
                    img.storagePath = finalPath;
                } else {
                    finalUrls[i] = img.url;
                }
            }
        } catch (e) {
            await sbRemove(bucket, movedFinalPaths);
            throw e;
        }

        // 4) draft 업데이트(최종 URL)
        try {
            const updatePayload = { ...payloadWithoutImageUrls, imageUrls: finalUrls.filter(Boolean) };
            await updateDraftFn(apiBase, createdAuctionId, updatePayload);
        } catch (e) {
            await sbRemove(bucket, movedFinalPaths);
            throw e;
        }

        // 5) temp 정리(실패해도 무방)
        await sbRemove(bucket, uploadedTempPaths);

        return createdAuctionId;
    }

    ns.storage = {
        getClient,
        parsePathFromPublicUrl,
        sbUpload,
        sbMove,
        sbCopy,
        sbRemove,
        buildFinalPath,
        buildTempPath,
        ensureFinalImageUrlsForExistingDraft,
        createDraftWithTempUploads,
    };
})();
