import { Api } from "./api.js";
import { Ui } from "./ui.js";
import { calcBidIncrement } from "./util.js";

(async function bootstrap() {
    const root = document.querySelector("#auction-detail-page");
    if (!root) return;

    const auctionId = root.dataset.auctionId;
    if (!auctionId) {
        alert("auctionId가 없습니다. (data-auction-id 확인)");
        return;
    }

    const ui = new Ui(root);
    const api = new Api();

    try {
        ui.setLoading(true);

        // 하위호환 메서드 사용 (현재 프로젝트에서 이 이름을 호출하고 있어도 안전)
        const { data, serverNowMs } = await api.fetchDetailWithServerTime(auctionId);

        // 서버에서 bidIncrement가 안 오면 프론트에서 계산(방어)
        if (data && (data.bidIncrement === null || data.bidIncrement === undefined)) {
            data.bidIncrement = calcBidIncrement(Number(data.currentPrice ?? 0));
        }

        ui.renderAll(data, serverNowMs);
        ui.bindInteractions(data, api);

        // 비슷한 경매 로드 (브랜드, 카테고리 기반)
        try {
            const brand = data?.specs?.brand || null;
            const category = data?.category || null;
            const { data: popularData } = await api.getPopularAuctions(0, 6, brand, category);
            const auctions = popularData?.content || [];
            ui.renderPopularAuctions(auctions, auctionId);
        } catch (e) {
            console.warn("비슷한 경매를 불러오지 못했습니다.", e);
            ui.renderPopularAuctions([], auctionId);
        }
    } catch (e) {
        console.error(e);
        ui.toastError("경매 정보를 불러오지 못했습니다.");
    } finally {
        ui.setLoading(false);
    }
})();
