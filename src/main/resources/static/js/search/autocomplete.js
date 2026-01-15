/**
 * 검색 자동완성 기능
 *
 * 기능:
 * 1. 타이핑 시 실시간 자동완성
 * 2. 포커스 시 인기 검색어 표시
 * 3. 키보드 네비게이션 (↑↓ Enter ESC)
 * 4. 검색어 선택 시 페이지 이동
 */

(function() {
    'use strict';

    // DOM 요소
    const searchInput = document.getElementById('searchInput');
    const dropdown = document.getElementById('autocompleteDropdown');
    const autocompleteResults = document.getElementById('autocompleteResults');
    const popularKeywords = document.getElementById('popularKeywords');

    if (!searchInput || !dropdown) return;

    // 상태
    let currentFocus = -1;
    let debounceTimer = null;
    let popularCache = null;

    // 초기화
    init();

    function init() {
        // 이벤트 리스너
        searchInput.addEventListener('input', handleInput);
        searchInput.addEventListener('focus', handleFocus);
        searchInput.addEventListener('keydown', handleKeydown);
        document.addEventListener('click', handleClickOutside);

        // 인기 검색어 미리 로드
        loadPopularKeywords();
    }

    /**
     * 입력 이벤트 핸들러
     */
    function handleInput(e) {
        const value = e.target.value.trim();

        clearTimeout(debounceTimer);

        if (value.length === 0) {
            showPopularKeywords();
            return;
        }

        // 300ms 디바운스
        debounceTimer = setTimeout(() => {
            fetchAutocomplete(value);
        }, 300);
    }

    /**
     * 포커스 이벤트 핸들러
     */
    function handleFocus() {
        const value = searchInput.value.trim();

        if (value.length === 0) {
            showPopularKeywords();
        } else {
            fetchAutocomplete(value);
        }
    }

    /**
     * 키보드 이벤트 핸들러
     */
    function handleKeydown(e) {
        const items = dropdown.querySelectorAll('.autocomplete-item');

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            currentFocus++;
            setActive(items);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            currentFocus--;
            setActive(items);
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (currentFocus > -1 && items[currentFocus]) {
                items[currentFocus].click();
            } else {
                submitSearch(searchInput.value.trim());
            }
        } else if (e.key === 'Escape') {
            hideDropdown();
            searchInput.blur();
        }
    }

    /**
     * 외부 클릭 핸들러
     */
    function handleClickOutside(e) {
        if (!searchInput.contains(e.target) && !dropdown.contains(e.target)) {
            hideDropdown();
        }
    }

    /**
     * 자동완성 API 호출
     */
    async function fetchAutocomplete(prefix) {
        try {
            const response = await fetch(`/api/search/autocomplete?q=${encodeURIComponent(prefix)}`);

            if (!response.ok) {
                throw new Error('자동완성 조회 실패');
            }

            const data = await response.json();
            renderAutocomplete(data);

        } catch (error) {
            console.error('자동완성 에러:', error);
            hideDropdown();
        }
    }

    /**
     * 인기 검색어 로드
     */
    async function loadPopularKeywords() {
        if (popularCache) return;

        try {
            const response = await fetch('/api/search/popular');

            if (!response.ok) {
                throw new Error('인기 검색어 조회 실패');
            }

            popularCache = await response.json();

        } catch (error) {
            console.error('인기 검색어 에러:', error);
        }
    }

    /**
     * 자동완성 결과 렌더링
     */
    function renderAutocomplete(results) {
        currentFocus = -1;

        if (!results || results.length === 0) {
            autocompleteResults.innerHTML = `
                <div class="px-4 py-3 text-sm text-gray-500">
                    검색 결과가 없습니다
                </div>
            `;
            popularKeywords.classList.add('hidden');
            showDropdown();
            return;
        }

        autocompleteResults.innerHTML = results.map(item => `
            <button type="button" 
                    class="autocomplete-item w-full text-left px-4 py-2 text-sm text-gray-900
                           hover:bg-gray-100 flex items-center justify-between"
                    data-keyword="${escapeHtml(item.keyword)}">
                <span>${highlightMatch(item.keyword, searchInput.value)}</span>
                <svg class="h-4 w-4 text-gray-400" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                </svg>
            </button>
        `).join('');

        // 클릭 이벤트 추가
        autocompleteResults.querySelectorAll('.autocomplete-item').forEach(item => {
            item.addEventListener('click', () => {
                const keyword = item.dataset.keyword;
                submitSearch(keyword);
            });
        });

        popularKeywords.classList.add('hidden');
        showDropdown();
    }

    /**
     * 인기 검색어 표시
     */
    function showPopularKeywords() {
        if (!popularCache || popularCache.length === 0) {
            hideDropdown();
            return;
        }

        currentFocus = -1;
        autocompleteResults.innerHTML = '';

        popularKeywords.innerHTML = `
            <div class="px-4 py-2 text-xs font-semibold text-gray-500">
                인기 검색어
            </div>
            ${popularCache.map((item, index) => `
                <button type="button"
                        class="autocomplete-item w-full text-left px-4 py-2 text-sm text-gray-900
                               hover:bg-gray-100 flex items-center gap-3"
                        data-keyword="${escapeHtml(item.keyword)}">
                    <span class="flex-shrink-0 w-5 h-5 flex items-center justify-center
                                 text-xs font-bold ${index < 3 ? 'text-red-500' : 'text-gray-400'}">
                        ${index + 1}
                    </span>
                    <span class="flex-1">${escapeHtml(item.keyword)}</span>
                </button>
            `).join('')}
        `;
        // 인기검색어 수
        /*<span class="text-xs text-gray-400">${item.searchCount.toLocaleString()}</span>*/

        // 클릭 이벤트 추가
        popularKeywords.querySelectorAll('.autocomplete-item').forEach(item => {
            item.addEventListener('click', () => {
                const keyword = item.dataset.keyword;
                submitSearch(keyword);
            });
        });

        popularKeywords.classList.remove('hidden');
        showDropdown();
    }

    /**
     * 검색 제출
     */
    function submitSearch(keyword) {
        if (!keyword || keyword.trim() === '') return;

        window.location.href = `/auctions?keyword=${encodeURIComponent(keyword.trim())}`;
    }

    /**
     * 키보드 네비게이션 활성화
     */
    function setActive(items) {
        if (!items || items.length === 0) return;

        // 범위 체크
        if (currentFocus >= items.length) currentFocus = 0;
        if (currentFocus < 0) currentFocus = items.length - 1;

        // 모든 항목 비활성화
        items.forEach(item => item.classList.remove('bg-gray-100'));

        // 현재 항목 활성화
        items[currentFocus].classList.add('bg-gray-100');
        items[currentFocus].scrollIntoView({ block: 'nearest' });
    }

    /**
     * 검색어 하이라이트
     */
    function highlightMatch(text, query) {
        if (!query) return escapeHtml(text);

        const escapedText = escapeHtml(text);
        const escapedQuery = escapeHtml(query);
        const regex = new RegExp(`(${escapedQuery})`, 'gi');

        return escapedText.replace(regex, '<strong class="font-semibold">$1</strong>');
    }

    /**
     * HTML 이스케이프
     */
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * 드롭다운 표시
     */
    function showDropdown() {
        dropdown.classList.remove('hidden');
    }

    /**
     * 드롭다운 숨김
     */
    function hideDropdown() {
        dropdown.classList.add('hidden');
        currentFocus = -1;
    }

})();