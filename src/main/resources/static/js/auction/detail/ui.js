import {
    formatNumber,
    safeText,
    shortUuid,
    formatEndAt,
    parseOffsetDateTimeToMs,
    computeOffsetMsFromServerNow,
    formatRemainingHms,
    startCountdown,
    resolveLabel,
    normalizeOrderedImages,
    calcBidIncrement
} from "./util.js";

export class Ui {
    constructor(root) {
        this.root = root;

        // root dataset
        this.auctionId = root?.dataset?.auctionId ?? null;
        this.meUserId = this.#normalizeUuid(root?.dataset?.meUserId ?? null);

        // overlay
        this.loadingOverlay = root.querySelector("#loading-overlay");

        // toast
        this.toast = root.querySelector("#toast");
        this.toastTitle = root.querySelector("#toastTitle");
        this.toastMsg = root.querySelector("#toastMsg");

        // header/title
        this.auctionTitle = root.querySelector("#auction-title");
        this.itemName = root.querySelector("#item-name");

        // unsold banner
        this.unsoldBanner = root.querySelector("#unsold-banner");

        // gallery
        this.mainImage = root.querySelector("#main-image");
        this.mainImageWrap = root.querySelector("#main-image-wrap");
        this.unsoldBadge = root.querySelector("#unsold-badge");
        this.myAuctionBadge = root.querySelector("#my-auction-badge");

        this.thumbs = root.querySelector("#thumbs");
        this.thumbPrevBtn = root.querySelector("#thumb-prev");
        this.thumbNextBtn = root.querySelector("#thumb-next");

        // detail
        this.desc = root.querySelector("#auction-description");
        this.descSection = root.querySelector("#description-section");
        this.detailItemName = root.querySelector("#detail-item-name");
        this.detailCondition = root.querySelector("#detail-condition");
        this.contentImages = root.querySelector("#content-images");

        // right panel
        this.priceCard = root.querySelector("#price-card");
        this.currentPrice = root.querySelector("#current-price");
        this.startPrice = root.querySelector("#start-price");

        this.remainingTime = root.querySelector("#remaining-time");
        this.endAt = root.querySelector("#end-at");
        this.auctionIdShort = root.querySelector("#auction-id-short");
        this.bidCount = root.querySelector("#bid-count");
        this.bidIncrement = root.querySelector("#bid-increment");

        // A) highest badge
        this.highestBadge = root.querySelector("#highest-badge");

        // my highest bid display
        this.myHighestBidAmount = root.querySelector("#my-highest-bid-amount");
        this.myBidValue = root.querySelector("#my-bid-value");

        // republish (seller action)
        this.republishBox = root.querySelector("#republish-box");
        this.republishButton = root.querySelector("#republish-button");
        this.republishHint = root.querySelector("#republish-hint");

        // cancel (seller action)
        this.cancelBox = root.querySelector("#cancel-box");
        this.cancelButton = root.querySelector("#cancel-button");

        // bid section wrapper
        this.bidFormSection = root.querySelector("#bid-form-section");

        // bid
        this.bidHint = root.querySelector("#bid-hint");
        this.bidInput = root.querySelector("#bid-input");
        this.bidMinus = root.querySelector("#bid-minus");
        this.bidPlus = root.querySelector("#bid-plus");
        this.bidQuickBtns = root.querySelectorAll(".bid-quick");
        this.bidError = root.querySelector("#bid-error");
        this.bidSubmit = root.querySelector("#bid-submit");

        // not highest warning
        this.notHighestWarning = root.querySelector("#not-highest-warning");
        this.minBidWarning = root.querySelector("#min-bid-warning");

        // C) highest hint
        this.highestHint = root.querySelector("#highest-hint");

        // confirmation toast
        this.confirmToast = root.querySelector("#confirm-toast");
        this.confirmToastTitle = root.querySelector("#confirmToastTitle");
        this.confirmToastMsg = root.querySelector("#confirmToastMsg");
        this.confirmToastOk = root.querySelector("#confirm-toast-ok");
        this.confirmToastCancel = root.querySelector("#confirm-toast-cancel");
        this.confirmToastOverlay = root.querySelector("#confirm-toast-overlay");

        // seller
        this.sellerAvatar = root.querySelector("#seller-avatar");
        this.sellerNickname = root.querySelector("#seller-nickname");
        this.sellerTradeCount = root.querySelector("#seller-trade-count");
        this.sellerCard = root.querySelector("#seller-card");

        // popular auctions
        this.popularAuctions = root.querySelector("#popular-auctions");

        // report modal
        this.reportButton = root.querySelector("#report-button");
        this.reportModal = root.querySelector("#report-modal");
        this.reportForm = root.querySelector("#report-form");
        this.reportCancel = root.querySelector("#report-cancel");
        this.reportDescription = root.querySelector("#report-description");
        this.reportReason = root.querySelector("#report-reason");

        // gallery state
        this.galleryUrls = [];
        this.mainImageIndex = 0;
        this.thumbWindowStart = 0;

        // countdown stopper
        this.stopCountdown = null;

        // bind once
        this._thumbNavBound = false;

        // latest data state
        this.data = null;

        // flash timer
        this._flashTimer = null;

        // previous highest bidder state (for detecting status change)
        this.wasHighestBidder = null;

        // prevent close refresh duplication
        this._closingLock = false;
    }

    setLoading(isLoading) {
        if (!this.loadingOverlay) return;
        this.loadingOverlay.classList.toggle("hidden", !isLoading);
        this.loadingOverlay.classList.toggle("flex", isLoading);
    }

    setToast(title, message) {
        if (!this.toast) return;
        if (this.toastTitle) this.toastTitle.textContent = title || "알림";
        if (this.toastMsg) this.toastMsg.textContent = message || "";

        this.toast.classList.remove("hidden");

        setTimeout(() => {
            this.toast.classList.add("hidden");
        }, 3000);
    }

    toastError(message) {
        this.setToast("오류", message);
    }

    renderAll(data, serverNowMs, api) {
        this.data = data;

        this.renderHeader(data);
        this.renderGallery(data);

        this.renderRightPanel(data);
        this.renderDetail(data);
        this.renderSeller(data);

        // 상태별 UI (유찰 포함) 먼저 반영
        this.applyAuctionStateUi(data);

        // countdown
        this.startAccurateCountdown(data, serverNowMs, api);

        // bid defaults (유찰이면 숨김이므로 내부에서 방어)
        this.prepareBidDefaults(data);

        // highest UI (LIVE가 아니거나 유찰이면 내부에서 숨김 처리)
        this.applyHighestUi(data);

        if (this.sellerCard && !this.sellerCard.dataset.bound) {
            this.sellerCard.addEventListener("click", () => {});
            this.sellerCard.dataset.bound = "1";
        }
    }

    renderHeader(data) {
        if (this.auctionTitle) this.auctionTitle.textContent = safeText(data.title);
        if (this.itemName) this.itemName.textContent = safeText(data.itemName);
    }

    renderGallery(data) {
        const directUrls = Array.isArray(data.imageUrls) ? data.imageUrls.filter(Boolean) : [];
        const normalized = normalizeOrderedImages(data).map((x) => x.url).filter(Boolean);
        const urls = directUrls.length ? directUrls : normalized;

        this.galleryUrls = urls;
        this.mainImageIndex = 0;
        this.thumbWindowStart = 0;

        const main = urls[0] || "/images/placeholder.png";
        this.setMainImage(main);

        if (!this._thumbNavBound) {
            this.thumbPrevBtn?.addEventListener("click", () => this.shiftThumbWindow(-5));
            this.thumbNextBtn?.addEventListener("click", () => this.shiftThumbWindow(5));
            this._thumbNavBound = true;
        }

        this.renderThumbWindow();
    }

    setMainImage(url) {
        if (!this.mainImage) return;
        this.mainImage.src = url;
    }

    renderThumbWindow() {
        if (!this.thumbs) return;

        const urls = Array.isArray(this.galleryUrls) ? this.galleryUrls : [];
        const windowSize = 5;

        const navEnabled = urls.length > windowSize;

        if (this.thumbPrevBtn) this.thumbPrevBtn.classList.toggle("hidden", !navEnabled);
        if (this.thumbNextBtn) this.thumbNextBtn.classList.toggle("hidden", !navEnabled);

        const maxStart = navEnabled ? Math.max(0, urls.length - windowSize) : 0;
        this.thumbWindowStart = Math.min(Math.max(0, this.thumbWindowStart), maxStart);

        if (navEnabled) {
            const atStart = this.thumbWindowStart <= 0;
            const atEnd = (this.thumbWindowStart + windowSize) >= urls.length;

            this.setNavVisualState(this.thumbPrevBtn, !atStart);
            this.setNavVisualState(this.thumbNextBtn, !atEnd);
        } else {
            this.setNavVisualState(this.thumbPrevBtn, false);
            this.setNavVisualState(this.thumbNextBtn, false);
        }

        this.thumbs.innerHTML = "";

        if (urls.length === 0) {
            const p = document.createElement("p");
            p.className = "text-xs text-slate-500";
            p.textContent = "등록된 이미지가 없습니다.";
            this.thumbs.appendChild(p);
            return;
        }

        const start = this.thumbWindowStart;
        const endExclusive = navEnabled ? Math.min(urls.length, start + windowSize) : urls.length;

        for (let globalIdx = start; globalIdx < endExclusive; globalIdx++) {
            const url = urls[globalIdx];
            const isActive = globalIdx === this.mainImageIndex;

            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = [
                "group relative h-14 w-14 sm:h-16 sm:w-16 overflow-hidden rounded-xl bg-white",
                "focus:outline-none focus:ring-2 focus:ring-slate-900",
                isActive ? "ring-2 ring-slate-900" : "ring-1 ring-slate-200 hover:ring-slate-400"
            ].join(" ");

            const img = document.createElement("img");
            img.src = url;
            img.alt = `썸네일 ${globalIdx + 1}`;
            img.className = "h-full w-full object-cover";

            btn.addEventListener("click", () => {
                this.mainImageIndex = globalIdx;
                this.setMainImage(url);

                if (navEnabled) {
                    this.ensureThumbWindowContains(this.mainImageIndex);
                } else {
                    this.thumbWindowStart = 0;
                }

                this.renderThumbWindow();
            });

            btn.appendChild(img);
            this.thumbs.appendChild(btn);
        }
    }

    setNavVisualState(buttonEl, enabled) {
        if (!buttonEl) return;
        buttonEl.dataset.enabled = enabled ? "1" : "0";
        buttonEl.classList.toggle("opacity-40", !enabled);
    }

    shiftThumbWindow(delta) {
        const urls = Array.isArray(this.galleryUrls) ? this.galleryUrls : [];
        const windowSize = 5;

        if (urls.length <= windowSize) return;

        const maxStart = Math.max(0, urls.length - windowSize);
        const nextStart = Math.min(Math.max(0, this.thumbWindowStart + delta), maxStart);

        if (nextStart === this.thumbWindowStart) return;

        this.thumbWindowStart = nextStart;
        this.renderThumbWindow();
    }

    ensureThumbWindowContains(index) {
        const urls = Array.isArray(this.galleryUrls) ? this.galleryUrls : [];
        const windowSize = 5;

        if (urls.length <= windowSize) {
            this.thumbWindowStart = 0;
            return;
        }

        if (index < this.thumbWindowStart) {
            this.thumbWindowStart = index;
            return;
        }
        if (index >= this.thumbWindowStart + windowSize) {
            this.thumbWindowStart = index - (windowSize - 1);
        }
    }

    renderRightPanel(data) {
        if (this.currentPrice) this.currentPrice.textContent = formatNumber(data.currentPrice);
        if (this.startPrice) this.startPrice.textContent = formatNumber(data.startPrice);

        if (this.bidCount) this.bidCount.textContent = String(data.bidCount ?? 0);
        if (this.bidIncrement) this.bidIncrement.textContent = formatNumber(data.bidIncrement ?? 0);

        if (this.endAt) this.endAt.textContent = formatEndAt(data.endAt);
        if (this.auctionIdShort) this.auctionIdShort.textContent = shortUuid(data.auctionId);
    }

    renderDetail(data) {
        if (this.detailItemName) this.detailItemName.textContent = safeText(data.itemName);
        if (this.detailCondition) this.detailCondition.textContent = resolveLabel("condition", data.condition);

        const hasDescription = data.description && String(data.description).trim();
        if (this.descSection) this.descSection.classList.toggle("hidden", !hasDescription);
        if (this.desc && hasDescription) this.desc.textContent = safeText(data.description);

        const directUrls = Array.isArray(data.imageUrls) ? data.imageUrls.filter(Boolean) : [];
        const ordered = normalizeOrderedImages(data).map((x) => x.url).filter(Boolean);
        const urls = directUrls.length ? directUrls : ordered;

        if (this.contentImages) {
            this.contentImages.innerHTML = "";

            urls.forEach((url, idx) => {
                const wrap = document.createElement("div");
                wrap.className = "flex justify-center overflow-hidden rounded-xl bg-white ring-1 ring-slate-200";

                const img = document.createElement("img");
                img.src = url;
                img.alt = `본문 이미지 ${idx + 1}`;
                img.className = "max-w-full h-auto";

                wrap.appendChild(img);
                this.contentImages.appendChild(wrap);
            });
        }
    }

    renderSeller(data) {
        if (this.sellerNickname) this.sellerNickname.textContent = safeText(data.sellerNickname);
        if (this.sellerAvatar) this.sellerAvatar.src = safeText(data.sellerProfileImageUrl, "/images/avatar-placeholder.png");
        if (this.sellerTradeCount) this.sellerTradeCount.textContent = String(data.sellerTradeCount ?? 0);
    }

    startAccurateCountdown(data, serverNowMs, api) {
        if (!this.remainingTime) return;

        const endAtMs = parseOffsetDateTimeToMs(data.endAt);
        if (!endAtMs) {
            this.remainingTime.textContent = "-";
            return;
        }

        const offsetMs = computeOffsetMsFromServerNow(serverNowMs);
        const getNowMs = () => Date.now() + offsetMs;

        if (this.stopCountdown) this.stopCountdown();

        this.stopCountdown = startCountdown({
            endAtMs,
            getNowMs,
            onTick: (diffMs) => {
                this.remainingTime.textContent = formatRemainingHms(diffMs);
            },
            onDone: () => {
                // 0초 도달 시 서버 마감 확정 후 최신 상태 반영(유찰 포함)
                this.handleCountdownDone(api);
            }
        });
    }

    async handleCountdownDone(api) {
        const d = this.data ?? {};
        if (!this.remainingTime) return;

        // 즉시 UI 표시(이후 서버 확정으로 갱신)
        this.remainingTime.textContent = "경매 종료";

        if (!api || this._closingLock) {
            this.updateBidButtonUi(d);
            this.applyAuctionStateUi(d);
            return;
        }

        this._closingLock = true;
        try {
            // 서버 마감 호출(멱등)
            await api.closeAuction(d.auctionId);

            // 최신 상태 재조회
            const { data: fresh, serverNowMs } = await api.fetchDetailWithServerTime(d.auctionId);

            if (fresh && (fresh.bidIncrement === null || fresh.bidIncrement === undefined)) {
                fresh.bidIncrement = calcBidIncrement(Number(fresh.currentPrice ?? 0));
            }

            this.data = fresh;

            // 주요 영역 갱신
            this.renderHeader(fresh);
            this.renderRightPanel(fresh);
            this.applyAuctionStateUi(fresh);
            this.applyHighestUi(fresh);

            // 만약 서버에서 연장 등으로 아직 LIVE라면, 새 endAt 기준으로 카운트다운 재시작
            if (String(fresh.status ?? "") === "LIVE") {
                this.prepareBidDefaults(fresh);
                this.startAccurateCountdown(fresh, serverNowMs, api);
                return;
            }

            // 종료 상태 라벨
            if (this.isUnsoldAuction(fresh)) {
                this.remainingTime.textContent = "유찰";
            } else if (this.isCancelledAuction(fresh)) {
                this.remainingTime.textContent = "경매 취소";
            } else {
                this.remainingTime.textContent = "경매 종료";
            }
            this.updateBidButtonUi(fresh);
        } catch (e) {
            console.warn("마감 확정/재조회 실패:", e);
            this.updateBidButtonUi(d);
            this.applyAuctionStateUi(d);
        } finally {
            this._closingLock = false;
        }
    }

    prepareBidDefaults(data) {
        // 유찰/취소이면 입찰 섹션 자체가 숨김이므로 여기서 종료
        if (this.isUnsoldAuction(data) || this.isCancelledAuction(data)) return;

        const minBid = this.computeMinBid(data);
        if (this.bidHint) this.bidHint.textContent = `(최소 ${formatNumber(minBid)}원 이상)`;

        this.setBidInputValue(minBid);
        this.updateBidButtonUi(data);
    }

    applyAuctionStateUi(data) {
        const unsold = this.isUnsoldAuction(data);
        const cancelled = this.isCancelledAuction(data);
        const isLive = String(data.status ?? "") === "LIVE";
        const isSeller = this.isSeller(data);

        // banner (유찰 또는 취소)
        if (this.unsoldBanner) {
            const showBanner = unsold || cancelled;
            this.unsoldBanner.classList.toggle("hidden", !showBanner);
            if (showBanner) {
                const bannerTitle = this.unsoldBanner.querySelector("p.font-extrabold");
                const bannerDesc = this.unsoldBanner.querySelector("p.mt-1");
                if (cancelled) {
                    if (bannerTitle) bannerTitle.textContent = "경매가 취소되었습니다";
                    if (bannerDesc) bannerDesc.textContent = "판매자가 경매를 취소했습니다.";
                } else {
                    if (bannerTitle) bannerTitle.textContent = "유찰된 경매입니다";
                    if (bannerDesc) bannerDesc.textContent = "이 경매는 입찰자가 없어 종료되었습니다.";
                }
            }
        }

        // gallery badge (유찰 또는 취소)
        if (this.unsoldBadge) {
            const showBadge = unsold || cancelled;
            this.unsoldBadge.classList.toggle("hidden", !showBadge);
            if (showBadge) {
                this.unsoldBadge.textContent = cancelled ? "경매 취소" : "유찰";
            }
        }

        // my auction badge (LIVE && isSeller)
        if (this.myAuctionBadge) {
            const showMyBadge = isLive && isSeller && !unsold && !cancelled;
            this.myAuctionBadge.classList.toggle("hidden", !showMyBadge);
        }

        // bid section: 판매자이거나 유찰/취소 시 숨김
        if (this.bidFormSection) {
            const hideBidSection = isSeller || unsold || cancelled;
            this.bidFormSection.classList.toggle("hidden", hideBidSection);
        }

        // republish button: seller & (unsold or cancelled)
        const canRepublish = isSeller && (unsold || cancelled);
        if (this.republishBox) {
            this.republishBox.classList.toggle("hidden", !canRepublish);
            // 힌트 문구: NO_BIDS일 때만 표시
            if (this.republishHint) {
                if (unsold) {
                    this.republishHint.classList.remove("hidden");
                    this.republishHint.textContent = "유찰된 경매를 수정해 다시 게시할 수 있습니다.";
                } else {
                    this.republishHint.classList.add("hidden");
                }
            }
        }

        // cancel button: seller & LIVE & bidCount === 0
        const canCancel = isSeller && isLive && (data.bidCount ?? 0) === 0;
        if (this.cancelBox) this.cancelBox.classList.toggle("hidden", !canCancel);

        // 종료 상태에서 최고입찰 UI들 정리
        if (unsold || cancelled) {
            this.hideBidErrorsAndWarnings();
            this.hideHighestUi();
        }
    }

    hideHighestUi() {
        this.highestBadge?.classList.add("hidden");
        this.highestHint?.classList.add("hidden");
        this.myHighestBidAmount?.classList.add("hidden");
        this.hideNotHighestWarning();
    }

    hideBidErrorsAndWarnings() {
        this.hideBidError();
        this.hideNotHighestWarning();
    }

    updateBidButtonUi(data) {
        if (!this.bidSubmit) return;

        // 유찰 또는 취소
        if (this.isUnsoldAuction(data)) {
            this.bidSubmit.disabled = true;
            this.bidSubmit.textContent = "유찰";
            return;
        }

        if (this.isCancelledAuction(data)) {
            this.bidSubmit.disabled = true;
            this.bidSubmit.textContent = "경매 취소";
            return;
        }

        const isLive = String(data.status ?? "") === "LIVE";
        this.bidSubmit.disabled = !isLive;

        if (!isLive) {
            this.bidSubmit.textContent = "경매 종료";
            this.hideHighestUi();
            return;
        }

        const isHighest = this.isHighestBidder(data);
        this.bidSubmit.textContent = isHighest ? "추가 입찰하기" : "입찰하기";
    }

    applyHighestUi(data) {
        // LIVE가 아니거나 유찰/취소이면 최고입찰 관련 UI 모두 숨김
        if (String(data.status ?? "") !== "LIVE" || this.isUnsoldAuction(data) || this.isCancelledAuction(data)) {
            this.hideHighestUi();
            return;
        }

        const isHighest = this.isHighestBidder(data);

        if (this.highestBadge) {
            this.highestBadge.classList.toggle("hidden", !isHighest);
        }

        if (this.highestHint) {
            this.highestHint.classList.toggle("hidden", !isHighest);
        }

        if (this.myHighestBidAmount && this.myBidValue) {
            if (isHighest) {
                this.myBidValue.textContent = formatNumber(data.currentPrice);
                this.myHighestBidAmount.classList.remove("hidden");
            } else {
                this.myHighestBidAmount.classList.add("hidden");
            }
        }

        if (this.wasHighestBidder === true && isHighest === false) {
            this.showNotHighestWarning(data);
            this.setToast("알림", "다른 사용자가 더 높은 금액을 입찰했습니다.");
        } else if (isHighest) {
            this.hideNotHighestWarning();
        }

        this.wasHighestBidder = isHighest;
        this.updateBidButtonUi(data);
    }

    isHighestBidder(data) {
        const me = this.meUserId;
        const highest = this.#normalizeUuid(data?.highestBidderId ?? null);
        if (!me || !highest) return false;
        return me === highest;
    }

    // 유찰 판정: status + endReason
    isUnsoldAuction(data) {
        const status = String(data?.status ?? "");
        const reason = String(data?.endReason ?? "");
        return status === "ENDED_UNSOLD" && reason === "NO_BIDS";
    }

    // 판매자 취소 판정: status + endReason
    isCancelledAuction(data) {
        const status = String(data?.status ?? "");
        const reason = String(data?.endReason ?? "");
        return status === "ENDED_UNSOLD" && reason === "SELLER_STOPPED";
    }

    // 판매자 판정(백엔드 필드명 차이 방어)
    isSeller(data) {
        const me = this.meUserId;
        if (!me) return false;

        const sellerId =
            data?.sellerId ??
            data?.sellerUserId ??
            data?.seller?.id ??
            data?.seller?.userId ??
            null;

        const s = this.#normalizeUuid(sellerId);
        if (!s) return false;

        return me === s;
    }

    computeMinBid(data) {
        const cur = Number(data.currentPrice ?? 0);
        const inc = Number(data.bidIncrement ?? 0);
        const start = Number(data.startPrice ?? 0);
        return Math.max(cur + inc, start);
    }

    bindInteractions(data, api) {
        this.data = data;

        // republish 버튼 바인딩(한 번만)
        if (this.republishButton && !this.republishButton.dataset.bound) {
            this.republishButton.addEventListener("click", async () => {
                const d = this.data ?? {};
                const unsold = this.isUnsoldAuction(d);
                const cancelled = this.isCancelledAuction(d);
                if (!unsold && !cancelled) return;
                if (!this.isSeller(d)) return;

                const confirmMsg = cancelled
                    ? "취소된 경매를 재등록하시겠습니까?"
                    : "유찰 경매를 재등록하시겠습니까?";
                const ok = window.confirm(confirmMsg);
                if (!ok) return;

                try {
                    this.setLoading(true);

                    const { data: repub } = await api.republishUnsoldAuction(d.auctionId);

                    // 응답 필드명 방어: newAuctionId(백엔드 실제 필드) 우선
                    const newId =
                        repub?.newAuctionId ??
                        repub?.auctionId ??
                        repub?.draftAuctionId ??
                        repub?.draftId ??
                        repub?.id ??
                        "";

                    if (!newId) {
                        throw new Error("재등록 응답에 이동할 ID가 없습니다.");
                    }

                    // 임시저장 수정 페이지로 이동
                    window.location.href = `/seller/auctions/drafts/${encodeURIComponent(newId)}`;
                } catch (e) {
                    this.toastError(e?.message || "재등록에 실패했습니다.");
                } finally {
                    this.setLoading(false);
                }
            });

            this.republishButton.dataset.bound = "1";
        }

        // cancel 버튼 바인딩(한 번만)
        if (this.cancelButton && !this.cancelButton.dataset.bound) {
            this.cancelButton.addEventListener("click", async () => {
                const d = this.data ?? {};
                const isLive = String(d.status ?? "") === "LIVE";
                const bidCount = d.bidCount ?? 0;

                if (!isLive) return;
                if (bidCount > 0) return;
                if (!this.isSeller(d)) return;

                const ok = window.confirm("경매를 취소하시겠습니까?");
                if (!ok) return;

                try {
                    this.setLoading(true);

                    await api.cancelAuctionBySeller(d.auctionId);

                    // 최신 상태 재조회
                    const { data: fresh, serverNowMs } = await api.fetchDetailWithServerTime(d.auctionId);

                    if (fresh && (fresh.bidIncrement === null || fresh.bidIncrement === undefined)) {
                        fresh.bidIncrement = calcBidIncrement(Number(fresh.currentPrice ?? 0));
                    }

                    this.data = fresh;

                    // UI 갱신
                    this.renderHeader(fresh);
                    this.renderRightPanel(fresh);
                    this.applyAuctionStateUi(fresh);
                    this.applyHighestUi(fresh);

                    this.setToast("경매 취소", "경매가 취소되었습니다. 재등록할 수 있습니다.");
                } catch (e) {
                    this.toastError(e?.message || "경매 취소에 실패했습니다.");
                } finally {
                    this.setLoading(false);
                }
            });

            this.cancelButton.dataset.bound = "1";
        }

        // Report Modal Interactions
        if (this.reportButton && !this.reportButton.dataset.bound) {
            this.reportButton.addEventListener("click", () => {
                if (this.reportModal) {
                    this.reportModal.classList.remove("hidden");
                    this.reportModal.classList.add("flex");
                }
            });
            this.reportButton.dataset.bound = "1";
        }

        if (this.reportCancel && !this.reportCancel.dataset.bound) {
            this.reportCancel.addEventListener("click", () => {
                if (this.reportModal) {
                    this.reportModal.classList.add("hidden");
                    this.reportModal.classList.remove("flex");
                    this.reportForm?.reset();
                }
            });
            this.reportCancel.dataset.bound = "1";
        }

        if (this.reportForm && !this.reportForm.dataset.bound) {
            this.reportForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                const d = this.data ?? {};
                const formData = new FormData(this.reportForm);
                const targetType = formData.get("reportTarget");
                const reason = this.reportReason?.value;
                const description = this.reportDescription?.value;

                if (!description || description.length < 10) {
                    alert("신고 내용은 최소 10자 이상 입력해주세요.");
                    return;
                }

                try {
                    this.setLoading(true);
                    await api.createReport({
                        auctionId: d.auctionId,
                        reportedUserId: d.sellerId || d.sellerUserId, // Assuming seller is reported
                        type: targetType,
                        reason: reason,
                        description: description
                    });
                    this.setToast("신고 접수", "신고가 정상적으로 접수되었습니다.");
                    this.reportModal.classList.add("hidden");
                    this.reportModal.classList.remove("flex");
                    this.reportForm.reset();
                } catch (err) {
                    this.toastError(err.message || "신고 접수에 실패했습니다.");
                } finally {
                    this.setLoading(false);
                }
            });
            this.reportForm.dataset.bound = "1";
        }

        // 유찰/취소이면 입찰 인터랙션 자체가 필요 없으므로, 아래 바인딩은 유지하되 실행 전 validate에서 방어
        this.bidMinus?.addEventListener("click", () => {
            const d = this.data ?? {};
            if (this.isUnsoldAuction(d) || this.isCancelledAuction(d)) return;
            this.adjustBidBy(-Number(d.bidIncrement ?? 0), d);
        });

        this.bidPlus?.addEventListener("click", () => {
            const d = this.data ?? {};
            if (this.isUnsoldAuction(d) || this.isCancelledAuction(d)) return;
            this.adjustBidBy(Number(d.bidIncrement ?? 0), d);
        });

        this.bidQuickBtns?.forEach((btn) => {
            btn.addEventListener("click", () => {
                const d = this.data ?? {};
                if (this.isUnsoldAuction(d) || this.isCancelledAuction(d)) return;
                const add = Number(btn.dataset.add ?? 0);
                this.adjustBidBy(add, d);
            });
        });

        this.bidInput?.addEventListener("input", () => {
            const d = this.data ?? {};
            if (this.isUnsoldAuction(d) || this.isCancelledAuction(d)) return;

            const n = this.readBidInputNumber();
            if (n === null) {
                this.bidInput.value = "";
                return;
            }
            this.setBidInputValue(n);
            this.validateBid(n, d);
        });

        // 입찰하기
        this.bidSubmit?.addEventListener("click", async () => {
            const userRole = this.bidSubmit.dataset.userRole;
            if (userRole === 'GUEST') {
                alert('전화번호 미인증 계정입니다. 전화번호 인증 후 이용 가능합니다.');
                return;
            }

            const d = this.data ?? {};
            if (this.isUnsoldAuction(d) || this.isCancelledAuction(d)) return;

            const n = this.readBidInputNumber();

            if (n === null) {
                this.showBidError("입찰 금액을 입력해 주세요.");
                return;
            }

            const ok = this.validateBid(n, d);
            if (!ok) return;

            const isHighest = this.isHighestBidder(d);
            if (isHighest) {
                const confirmed = await this.showConfirmToast(
                    "현재 최고 입찰자입니다",
                    "더 높은 금액으로 추가 입찰하시겠습니까?"
                );
                if (!confirmed) return;
            }

            const originalText = this.bidSubmit.textContent;
            const oldEndAtMs = parseOffsetDateTimeToMs(d.endAt);

            this.bidSubmit.disabled = true;
            this.bidSubmit.textContent = "입찰 중...";
            this.hideBidError();
            this.hideNotHighestWarning();

            try {
                const clientRequestId = crypto.randomUUID();
                await api.createBid(d.auctionId, n, clientRequestId);

                const { data: fresh, serverNowMs } = await api.fetchDetailWithServerTime(d.auctionId);

                if (fresh && (fresh.bidIncrement === null || fresh.bidIncrement === undefined)) {
                    fresh.bidIncrement = calcBidIncrement(Number(fresh.currentPrice ?? 0));
                }

                const newEndAtMs = parseOffsetDateTimeToMs(fresh.endAt);
                const extended = (oldEndAtMs && newEndAtMs && newEndAtMs > oldEndAtMs);

                this.setToast(
                    "입찰 성공",
                    extended
                        ? "입찰이 반영되었고 종료 시간이 연장되었습니다."
                        : "입찰이 반영되었습니다."
                );

                this.flashPriceCard();
                await this.temporarilySetBidButtonText("입찰 완료", 900);

                this.data = fresh;

                this.renderRightPanel(fresh);
                this.applyAuctionStateUi(fresh);
                this.startAccurateCountdown(fresh, serverNowMs, api);
                this.prepareBidDefaults(fresh);
                this.applyHighestUi(fresh);
            } catch (e) {
                this.toastError(e.message || "입찰에 실패했습니다.");
            } finally {
                const latest = this.data ?? data;

                if (String(latest.status ?? "") === "LIVE" && !this.isUnsoldAuction(latest) && !this.isCancelledAuction(latest)) {
                    this.updateBidButtonUi(latest);
                } else if (this.bidSubmit) {
                    if (this.isUnsoldAuction(latest)) {
                        this.bidSubmit.textContent = "유찰";
                    } else if (this.isCancelledAuction(latest)) {
                        this.bidSubmit.textContent = "경매 취소";
                    } else {
                        this.bidSubmit.textContent = "경매 종료";
                    }
                }

                this.updateBidButtonUi(this.data ?? data);

                if (this.bidSubmit && this.bidSubmit.textContent === "입찰 중...") {
                    this.bidSubmit.textContent = originalText;
                }
            }
        });
    }

    async temporarilySetBidButtonText(text, durationMs) {
        if (!this.bidSubmit) return;
        const prev = this.bidSubmit.textContent;

        this.bidSubmit.textContent = text;
        await new Promise((r) => setTimeout(r, Math.max(0, Number(durationMs ?? 0))));
        this.bidSubmit.textContent = prev;
    }

    flashPriceCard() {
        if (!this.priceCard) return;

        if (this._flashTimer) {
            clearTimeout(this._flashTimer);
            this._flashTimer = null;
        }

        this.priceCard.classList.add("ring-2", "ring-slate-900", "animate-pulse");

        this._flashTimer = setTimeout(() => {
            this.priceCard.classList.remove("animate-pulse");
            this.priceCard.classList.remove("ring-2", "ring-slate-900");
            this._flashTimer = null;
        }, 1500);
    }

    adjustBidBy(delta, data) {
        const cur = this.readBidInputNumber();
        const base = (cur === null) ? this.computeMinBid(data) : cur;
        const next = Math.max(this.computeMinBid(data), base + delta);

        this.setBidInputValue(next);
        this.validateBid(next, data);
    }

    validateBid(amount, data) {
        // 유찰/취소이면 항상 false
        if (this.isUnsoldAuction(data)) {
            this.showBidError("유찰된 경매는 입찰할 수 없습니다.");
            return false;
        }

        if (this.isCancelledAuction(data)) {
            this.showBidError("취소된 경매는 입찰할 수 없습니다.");
            return false;
        }

        const minBid = this.computeMinBid(data);
        const inc = Number(data.bidIncrement ?? 0);

        if (amount < minBid) {
            this.showBidError(`최소 입찰 금액은 ${formatNumber(minBid)}원입니다.`);
            return false;
        }

        if (inc > 0) {
            const diff = amount - minBid;
            if (diff % inc !== 0) {
                this.showBidError(`입찰 단위(+${formatNumber(inc)}원)에 맞춰 입력해 주세요.`);
                return false;
            }
        }

        this.hideBidError();
        return true;
    }

    readBidInputNumber() {
        if (!this.bidInput) return null;
        const raw = (this.bidInput.value ?? "").replaceAll(",", "").trim();
        if (!raw) return null;
        if (!/^\d+$/.test(raw)) return null;
        return Number(raw);
    }

    setBidInputValue(n) {
        if (!this.bidInput) return;
        this.bidInput.value = formatNumber(n);
    }

    showBidError(msg) {
        if (!this.bidError) return;
        this.bidError.textContent = msg;
        this.bidError.classList.remove("hidden");
    }

    hideBidError() {
        if (!this.bidError) return;
        this.bidError.textContent = "";
        this.bidError.classList.add("hidden");
    }

    showNotHighestWarning(data) {
        if (!this.notHighestWarning || !this.minBidWarning) return;
        const minBid = this.computeMinBid(data);
        this.minBidWarning.textContent = formatNumber(minBid);
        this.notHighestWarning.classList.remove("hidden");
    }

    hideNotHighestWarning() {
        if (!this.notHighestWarning) return;
        this.notHighestWarning.classList.add("hidden");
    }

    showConfirmToast(title, message) {
        return new Promise((resolve) => {
            if (!this.confirmToast || !this.confirmToastOverlay) {
                resolve(false);
                return;
            }

            if (this.confirmToastTitle) this.confirmToastTitle.textContent = title || "확인";
            if (this.confirmToastMsg) this.confirmToastMsg.textContent = message || "";

            this.confirmToastOverlay.classList.remove("hidden");
            this.confirmToast.classList.remove("hidden");

            const onOk = () => {
                this.hideConfirmToast();
                cleanup();
                resolve(true);
            };

            const onCancel = () => {
                this.hideConfirmToast();
                cleanup();
                resolve(false);
            };

            const cleanup = () => {
                this.confirmToastOk?.removeEventListener("click", onOk);
                this.confirmToastCancel?.removeEventListener("click", onCancel);
                this.confirmToastOverlay?.removeEventListener("click", onCancel);
            };

            this.confirmToastOk?.addEventListener("click", onOk);
            this.confirmToastCancel?.addEventListener("click", onCancel);
            this.confirmToastOverlay?.addEventListener("click", onCancel);
        });
    }

    hideConfirmToast() {
        if (!this.confirmToast || !this.confirmToastOverlay) return;
        this.confirmToast.classList.add("hidden");
        this.confirmToastOverlay.classList.add("hidden");
    }

    renderPopularAuctions(auctions, currentAuctionId) {
        if (!this.popularAuctions) return;

        this.popularAuctions.innerHTML = "";

        const filteredAuctions = (auctions || []).filter(
            auction => auction.auctionId !== currentAuctionId
        );

        if (filteredAuctions.length === 0) return;

        filteredAuctions.forEach((auction) => {
            const card = document.createElement("a");
            card.href = `/auctions/${auction.auctionId}`;
            card.className = "group block flex-shrink-0 w-40 overflow-hidden rounded-xl bg-white shadow-sm ring-1 ring-slate-200 transition-all hover:shadow-md hover:ring-slate-300";

            const imgWrap = document.createElement("div");
            imgWrap.className = "aspect-square overflow-hidden bg-slate-50";

            const img = document.createElement("img");
            img.src = auction.thumbnailUrl || "/images/placeholder.png";
            img.alt = safeText(auction.title);
            img.className = "h-full w-full object-cover transition-transform group-hover:scale-105";

            imgWrap.appendChild(img);
            card.appendChild(imgWrap);

            const info = document.createElement("div");
            info.className = "p-3";

            const title = document.createElement("h3");
            title.className = "truncate text-xs font-extrabold text-slate-900";
            title.textContent = safeText(auction.title);

            const price = document.createElement("p");
            price.className = "mt-1.5 text-sm font-extrabold text-slate-900";
            price.textContent = `${formatNumber(auction.currentPrice)}원`;

            info.appendChild(title);
            info.appendChild(price);

            card.appendChild(info);
            this.popularAuctions.appendChild(card);
        });
    }

    #normalizeUuid(v) {
        if (v === null || v === undefined) return null;
        const s = String(v).trim();
        if (!s) return null;
        return s.toLowerCase();
    }
}
