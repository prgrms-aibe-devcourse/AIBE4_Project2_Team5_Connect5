// 모든 드롭다운 메뉴를 닫는 함수
function closeAllMenus() {
    const menus = ['profileMenu', 'sellerMenu', 'purchaseMenu'];
    menus.forEach(menuId => {
        const menu = document.getElementById(menuId);
        if (menu) {
            menu.classList.add('hidden');
        }
    });
}

function toggleProfileMenu() {
    const menu = document.getElementById('profileMenu');
    if (!menu) return;

    const isCurrentlyHidden = menu.classList.contains('hidden');
    closeAllMenus();

    if (isCurrentlyHidden) {
        menu.classList.remove('hidden');
    }
}

function toggleSellerMenu() {
    const menu = document.getElementById('sellerMenu');
    if (!menu) return;

    const isCurrentlyHidden = menu.classList.contains('hidden');
    closeAllMenus();

    if (isCurrentlyHidden) {
        menu.classList.remove('hidden');
    }
}

function togglePurchaseMenu() {
    const menu = document.getElementById('purchaseMenu');
    if (!menu) return;

    const isCurrentlyHidden = menu.classList.contains('hidden');
    closeAllMenus();

    if (isCurrentlyHidden) {
        menu.classList.remove('hidden');
    }
}

function toggleProfileMega() {
    const panel = document.getElementById('profileMega');
    const overlay = document.getElementById('profileMegaOverlay');
    const btn = document.getElementById('profileButton');
    if (!panel || !overlay || !btn) return;

    const isOpen = !panel.classList.contains('hidden');
    if (isOpen) closeProfileMega();
    else openProfileMega();
}

function openProfileMega() {
    const panel = document.getElementById('profileMega');
    const overlay = document.getElementById('profileMegaOverlay');
    const btn = document.getElementById('profileButton');
    if (!panel || !overlay || !btn) return;

    panel.classList.remove('hidden');
    overlay.classList.remove('hidden');
    btn.setAttribute('aria-expanded', 'true');
}

function closeProfileMega() {
    const panel = document.getElementById('profileMega');
    const overlay = document.getElementById('profileMegaOverlay');
    const btn = document.getElementById('profileButton');
    if (!panel || !overlay || !btn) return;

    panel.classList.add('hidden');
    overlay.classList.add('hidden');
    btn.setAttribute('aria-expanded', 'false');
}

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeProfileMega();
});

document.addEventListener('click', (e) => {
    const panel = document.getElementById('profileMega');
    const btn = document.getElementById('profileButton');
    if (!panel || !btn) return;

    const overlay = document.getElementById('profileMegaOverlay');
    const isOpen = !panel.classList.contains('hidden');
    if (!isOpen) return;

    const clickedInside = panel.contains(e.target) || btn.contains(e.target);
    if (!clickedInside) closeProfileMega();
});

document.addEventListener('click', (e) => {
    const profileBtn = document.getElementById('profileButton');
    const profileMenu = document.getElementById('profileMenu');
    const sellerWrapper = document.getElementById('sellerMenuWrapper');
    const purchaseWrapper = document.getElementById('purchaseMenuWrapper');

    // 모든 드롭다운 wrapper/button 확인
    const isClickInsideAnyDropdown =
        (profileBtn && profileBtn.contains(e.target)) ||
        (profileMenu && profileMenu.contains(e.target)) ||
        (sellerWrapper && sellerWrapper.contains(e.target)) ||
        (purchaseWrapper && purchaseWrapper.contains(e.target));

    // 외부 클릭 시 모든 메뉴 닫기
    if (!isClickInsideAnyDropdown) {
        closeAllMenus();
    }
}

);
