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
    normalizeOrderedImages
} from "./util.js";

export class Ui {
    constructor(root) {
        this.root = root;

        // overlay
        this.loadingOverlay = root.querySelector("#loading-overlay");

        // header/title
        this.auctionTitle = root.querySelector("#auction-title");
        this.itemName = root.querySelector("#item-name");

        // gallery
        this.mainImage = root.querySelector("#main-image");
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
        this.currentPrice = root.querySelector("#current-price");
        this.startPrice = root.querySelector("#start-price");

        this.remainingTime = root.querySelector("#remaining-time");
        this.endAt = root.querySelector("#end-at");
        this.auctionIdShort = root.querySelector("#auction-id-short");
        this.bidCount = root.querySelector("#bid-count");
        this.bidIncrement = root.querySelector("#bid-increment");

        // bid
        this.bidHint = root.querySelector("#bid-hint");
        this.bidInput = root.querySelector("#bid-input");
        this.bidMinus = root.querySelector("#bid-minus");
        this.bidPlus = root.querySelector("#bid-plus");
        this.bidQuickBtns = root.querySelectorAll(".bid-quick");
        this.bidError = root.querySelector("#bid-error");
        this.bidSubmit = root.querySelector("#bid-submit");

        // seller
        this.sellerAvatar = root.querySelector("#seller-avatar");
        this.sellerNickname = root.querySelector("#seller-nickname");
        this.sellerTradeCount = root.querySelector("#seller-trade-count");
        this.sellerCard = root.querySelector("#seller-card");

        // popular auctions
        this.popularAuctions = root.querySelector("#popular-auctions");

        // gallery state
        this.galleryUrls = [];
        this.mainImageIndex = 0;
        this.thumbWindowStart = 0;

        // countdown stopper
        this.stopCountdown = null;

        // bind once
        this._thumbNavBound = false;
    }

    setLoading(isLoading) {
        if (!this.loadingOverlay) return;
        this.loadingOverlay.classList.toggle("hidden", !isLoading);
        this.loadingOverlay.classList.toggle("flex", isLoading);
    }

    toastError(message) {
        alert(message);
    }

    renderAll(data, serverNowMs) {
        this.renderHeader(data);
        this.renderGallery(data);

        this.renderRightPanel(data);
        this.renderDetail(data);
        this.renderSeller(data);

        this.startAccurateCountdown(data, serverNowMs);
        this.prepareBidDefaults(data);

        // 모달(추후) 자리: 현재는 아무 동작 안 함
        if (this.sellerCard && !this.sellerCard.dataset.bound) {
            this.sellerCard.addEventListener("click", () => {
                // placeholder
            });
            this.sellerCard.dataset.bound = "1";
        }
    }

    renderHeader(data) {
        if (this.auctionTitle) this.auctionTitle.textContent = safeText(data.title);
        if (this.itemName) this.itemName.textContent = safeText(data.itemName);
    }

    /**
     * 썸네일 규칙
     * - 썸네일은 "최대 5개 창"으로 보여주되, 실제 이미지 개수만 렌더(빈칸 금지)
     * - 이미지가 6장 이상일 때만 <, > 버튼 노출/이동
     * - 끝에 도달하면 버튼은 흐리게(비활성처럼)만 보이게 하고, disabled 속성은 사용하지 않음(금지 커서 방지)
     */
    renderGallery(data) {
        // 상단 갤러리용 URL 우선순위:
        // 1) data.imageUrls(문자열 배열)
        // 2) normalizeOrderedImages()로 추출한 url 목록
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

        // 버튼은 "6장 이상"일 때만 노출
        if (this.thumbPrevBtn) this.thumbPrevBtn.classList.toggle("hidden", !navEnabled);
        if (this.thumbNextBtn) this.thumbNextBtn.classList.toggle("hidden", !navEnabled);

        // start 보정
        const maxStart = navEnabled ? Math.max(0, urls.length - windowSize) : 0;
        this.thumbWindowStart = Math.min(Math.max(0, this.thumbWindowStart), maxStart);

        // 끝에서 비활성처럼 보이게 (disabled 속성 금지)
        if (navEnabled) {
            const atStart = this.thumbWindowStart <= 0;
            const atEnd = (this.thumbWindowStart + windowSize) >= urls.length;

            this.setNavVisualState(this.thumbPrevBtn, !atStart);
            this.setNavVisualState(this.thumbNextBtn, !atEnd);
        } else {
            this.setNavVisualState(this.thumbPrevBtn, false);
            this.setNavVisualState(this.thumbNextBtn, false);
        }

        // 실제 존재하는 썸네일만 렌더(빈칸 X)
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

        // enabled=false면 흐리게 + 클릭 무시되도록 dataset으로만 제어(disabled 사용 X)
        buttonEl.dataset.enabled = enabled ? "1" : "0";
        buttonEl.classList.toggle("opacity-40", !enabled);
    }

    shiftThumbWindow(delta) {
        const urls = Array.isArray(this.galleryUrls) ? this.galleryUrls : [];
        const windowSize = 5;

        if (urls.length <= windowSize) return;

        const maxStart = Math.max(0, urls.length - windowSize);
        const nextStart = Math.min(Math.max(0, this.thumbWindowStart + delta), maxStart);

        // 버튼이 흐린 상태(끝)에서 클릭해도 이동하지 않도록(스무스)
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

        // 설명이 있을 때만 섹션 표시
        const hasDescription = data.description && String(data.description).trim();
        if (this.descSection) {
            this.descSection.classList.toggle("hidden", !hasDescription);
        }
        if (this.desc && hasDescription) {
            this.desc.textContent = safeText(data.description);
        }

        // 하단 “사진 쭈루룩”
        // - imageUrls 문자열 배열이 있으면 그대로 사용
        // - 없으면 normalizeOrderedImages로 추출/정렬된 목록 사용
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

    startAccurateCountdown(data, serverNowMs) {
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
                this.remainingTime.textContent = "경매 종료";
            }
        });
    }

    prepareBidDefaults(data) {
        const minBid = this.computeMinBid(data);
        if (this.bidHint) this.bidHint.textContent = `(최소 ${formatNumber(minBid)}원 이상)`;
        this.setBidInputValue(minBid);

        const canBid = data.status === "LIVE";
        if (this.bidSubmit) this.bidSubmit.disabled = !canBid;
    }

    computeMinBid(data) {
        const cur = Number(data.currentPrice ?? 0);
        const inc = Number(data.bidIncrement ?? 0);
        const start = Number(data.startPrice ?? 0);
        return Math.max(cur + inc, start);
    }

    bindInteractions(data, api) {
        // bid +/- 버튼
        this.bidMinus?.addEventListener("click", () => {
            this.adjustBidBy(-Number(data.bidIncrement ?? 0), data);
        });
        this.bidPlus?.addEventListener("click", () => {
            this.adjustBidBy(Number(data.bidIncrement ?? 0), data);
        });

        // quick add
        this.bidQuickBtns?.forEach((btn) => {
            btn.addEventListener("click", () => {
                const add = Number(btn.dataset.add ?? 0);
                this.adjustBidBy(add, data);
            });
        });

        // 입력 포맷팅
        this.bidInput?.addEventListener("input", () => {
            const n = this.readBidInputNumber();
            if (n === null) {
                this.bidInput.value = "";
                return;
            }
            this.setBidInputValue(n);
            this.validateBid(n, data);
        });

        // 입찰하기(현재는 UI만)
        this.bidSubmit?.addEventListener("click", () => {
            const n = this.readBidInputNumber();
            if (n === null) {
                this.showBidError("입찰 금액을 입력해 주세요.");
                return;
            }
            const ok = this.validateBid(n, data);
            if (!ok) return;

            alert("입찰 API 연결 전입니다. (UI 구성 완료)");
        });
    }

    adjustBidBy(delta, data) {
        const cur = this.readBidInputNumber();
        const base = (cur === null) ? this.computeMinBid(data) : cur;
        const next = Math.max(this.computeMinBid(data), base + delta);

        this.setBidInputValue(next);
        this.validateBid(next, data);
    }

    validateBid(amount, data) {
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

    renderPopularAuctions(auctions, currentAuctionId) {
        if (!this.popularAuctions) return;

        this.popularAuctions.innerHTML = "";

        // 자기 자신을 제외
        const filteredAuctions = (auctions || []).filter(
            auction => auction.auctionId !== currentAuctionId
        );

        // 비슷한 경매가 없으면 빈 칸으로 둠
        if (filteredAuctions.length === 0) {
            return;
        }

        filteredAuctions.forEach((auction) => {
            const card = document.createElement("a");
            card.href = `/auctions/${auction.auctionId}`;
            card.className = "group block flex-shrink-0 w-40 overflow-hidden rounded-xl bg-white shadow-sm ring-1 ring-slate-200 transition-all hover:shadow-md hover:ring-slate-300";

            // 이미지
            const imgWrap = document.createElement("div");
            imgWrap.className = "aspect-square overflow-hidden bg-slate-50";

            const img = document.createElement("img");
            img.src = auction.thumbnailUrl || "/images/placeholder.png";
            img.alt = safeText(auction.title);
            img.className = "h-full w-full object-cover transition-transform group-hover:scale-105";

            imgWrap.appendChild(img);
            card.appendChild(imgWrap);

            // 정보
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
}
