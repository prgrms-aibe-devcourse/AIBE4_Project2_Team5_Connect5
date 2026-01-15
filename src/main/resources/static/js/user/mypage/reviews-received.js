document.addEventListener('DOMContentLoaded', async () => {
    const listEl = document.getElementById('reviewList');
    const emptyEl = document.getElementById('emptyMessage');

    try {
        const res = await fetch('/api/seller/reviews');
        if (!res.ok) {
            throw new Error('받은 리뷰 목록을 불러오지 못했습니다.');
        }

        const reviews = await res.json();

        if (!reviews || reviews.length === 0) {
            emptyEl.classList.remove('hidden');
            return;
        }

        reviews.forEach(r => {
            const item = document.createElement('div');
            item.className = 'border rounded-lg bg-white p-4 hover:bg-gray-50 cursor-pointer';

            const createdAt = r.createdAt ?? '';
            const rating = r.rating ?? '-';
            const targetNickname = r.targetNickname ?? '작성자';
            const preview = (r.content ?? '').length > 60
                ? (r.content.substring(0, 60) + '...')
                : (r.content ?? '');

            item.innerHTML = `
                <a href="/mypage/reviews/received/${r.reviewId}">
                    <div class="flex justify-between items-center mb-1">
                        <div class="text-sm text-gray-500">${createdAt}</div>
                        <div class="text-xs text-yellow-500 font-semibold">★ ${rating}</div>
                    </div>
                    <div class="text-sm text-gray-700 mb-1">
                        작성자: <span class="font-medium">${targetNickname}</span>
                    </div>
                    <div class="text-sm text-gray-900">
                        ${preview || '내용 없음'}
                    </div>
                </a>
            `;

            listEl.appendChild(item);
        });
    } catch (e) {
        console.error(e);
        emptyEl.textContent = '목록을 불러오는 중 오류가 발생했습니다.';
        emptyEl.classList.remove('hidden');
    }
});
