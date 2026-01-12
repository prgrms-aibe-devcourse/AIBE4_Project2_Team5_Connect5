(async function() {
    // API 호출 함수
    async function fetchAuctions(sort, size = 4) {
        const params = new URLSearchParams({
            page: "0",
            size: String(size),
            sortKey: sort,
            status: "LIVE"
        });

        const response = await fetch(`/api/auctions/list?${params.toString()}`, {
            method: "GET",
            headers: { Accept: "application/json" }
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch auctions: ${response.status}`);
        }

        const data = await response.json();
        return data.content || [];
    }

    // 숫자 포맷팅 (1000 -> 1,000)
    function formatNumber(n) {
        if (n == null || n === "") return "0";
        return String(n).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    }

    // 남은 시간 계산 (사람이 읽기 쉬운 형태)
    function formatRemainingTime(endAt) {
        try {
            const endTime = new Date(endAt).getTime();
            const now = Date.now();
            const diff = endTime - now;

            if (diff <= 0) return "종료";

            const minutes = Math.floor(diff / 60000);
            const hours = Math.floor(minutes / 60);
            const days = Math.floor(hours / 24);

            if (days > 0) {
                const remainingHours = hours % 24;
                return `${days}일 ${remainingHours}시간`;
            } else if (hours > 0) {
                const remainingMinutes = minutes % 60;
                return `${hours}시간 ${remainingMinutes}분`;
            } else {
                return `${minutes}분`;
            }
        } catch (e) {
            return "-";
        }
    }

    // 경매 카드 렌더링
    function renderAuctionCard(auction) {
        const article = document.createElement("article");
        article.className = "group overflow-hidden rounded-2xl border border-gray-200 bg-white hover:shadow-md transition";

        // 이미지
        const imageDiv = document.createElement("div");
        imageDiv.className = "relative aspect-square bg-gray-100 overflow-hidden";

        const img = document.createElement("img");
        img.src = auction.thumbnailUrl || "/images/placeholder.png";
        img.alt = auction.title || "경매 이미지";
        img.className = "h-full w-full object-cover";

        imageDiv.appendChild(img);

        // 정보
        const infoDiv = document.createElement("div");
        infoDiv.className = "p-4";

        // 제목
        const title = document.createElement("div");
        title.className = "text-sm font-semibold text-gray-900 line-clamp-2";
        title.textContent = auction.title || "제목 없음";

        // 메타 정보 (입찰 횟수, 남은 시간)
        const metaDiv = document.createElement("div");
        metaDiv.className = "mt-3 flex items-center justify-between text-xs text-gray-500";

        const bidCount = document.createElement("span");
        bidCount.textContent = `입찰 ${auction.bidCount ?? 0}회`;

        const timeSpan = document.createElement("span");
        timeSpan.className = "flex items-center gap-1 font-semibold text-slate-900";

        const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
        svg.setAttribute("class", "h-3.5 w-3.5");
        svg.setAttribute("fill", "none");
        svg.setAttribute("stroke", "currentColor");
        svg.setAttribute("stroke-width", "2");
        svg.setAttribute("viewBox", "0 0 24 24");

        const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
        path.setAttribute("stroke-linecap", "round");
        path.setAttribute("stroke-linejoin", "round");
        path.setAttribute("d", "M12 6v6l4 2");

        const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
        circle.setAttribute("cx", "12");
        circle.setAttribute("cy", "12");
        circle.setAttribute("r", "9");

        svg.appendChild(path);
        svg.appendChild(circle);

        timeSpan.appendChild(svg);
        timeSpan.appendChild(document.createTextNode(formatRemainingTime(auction.endAt)));

        metaDiv.appendChild(bidCount);
        metaDiv.appendChild(timeSpan);

        // 가격
        const priceDiv = document.createElement("div");
        priceDiv.className = "mt-1 text-base font-extrabold text-gray-900";
        priceDiv.textContent = `${formatNumber(auction.currentPrice)}원`;

        infoDiv.appendChild(title);
        infoDiv.appendChild(metaDiv);
        infoDiv.appendChild(priceDiv);

        // 링크로 감싸기
        const link = document.createElement("a");
        link.href = `/auctions/${auction.auctionId}`;
        link.appendChild(imageDiv);
        link.appendChild(infoDiv);

        article.appendChild(link);

        return article;
    }

    // 경매 목록 렌더링
    function renderAuctions(container, auctions) {
        container.innerHTML = "";

        if (!auctions || auctions.length === 0) {
            const msg = document.createElement("p");
            msg.className = "col-span-full text-center text-sm text-gray-500 py-8";
            msg.textContent = "현재 진행 중인 경매가 없습니다.";
            container.appendChild(msg);
            return;
        }

        auctions.forEach(auction => {
            const card = renderAuctionCard(auction);
            container.appendChild(card);
        });
    }

    // 초기화
    try {
        // 인기 경매 로드
        const popularContainer = document.querySelector("#popular-auctions");
        if (popularContainer) {
            const popularAuctions = await fetchAuctions("popular", 4);
            renderAuctions(popularContainer, popularAuctions);
        }

        // 마감 임박 경매 로드
        const closingContainer = document.querySelector("#closing-auctions");
        if (closingContainer) {
            const closingAuctions = await fetchAuctions("deadline", 4);
            renderAuctions(closingContainer, closingAuctions);
        }
    } catch (error) {
        console.error("경매 목록을 불러오는데 실패했습니다:", error);
    }
})();
