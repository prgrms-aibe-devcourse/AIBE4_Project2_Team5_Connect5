// /static/js/auction-list.js
document.addEventListener("DOMContentLoaded", () => {
    const minInput = document.getElementById("minPriceInput");
    const maxInput = document.getElementById("maxPriceInput");
    const form = document.querySelector('form[method="get"]');

    if (!minInput || !maxInput || !form) return;

    const numberFormatter = new Intl.NumberFormat("ko-KR");

    // 숫자만 남기고 콤마 붙이기
    function formatWithComma(el) {
        const raw = el.value.replace(/[^\d]/g, ""); // 숫자만
        if (raw.length === 0) {
            el.value = "";
            return;
        }
        // 너무 큰 값 방지(선택): 필요 없으면 제거
        // const safe = raw.slice(0, 12);

        el.value = numberFormatter.format(Number(raw));
    }

    // submit 전에 콤마 제거해서 서버에는 숫자만 가게
    function stripComma(el) {
        el.value = el.value.replace(/[^\d]/g, "");
    }

    // 입력할 때마다 콤마 포맷
    minInput.addEventListener("input", () => formatWithComma(minInput));
    maxInput.addEventListener("input", () => formatWithComma(maxInput));

    // 폼 전송 직전 콤마 제거
    form.addEventListener("submit", () => {
        stripComma(minInput);
        stripComma(maxInput);
    });
});
