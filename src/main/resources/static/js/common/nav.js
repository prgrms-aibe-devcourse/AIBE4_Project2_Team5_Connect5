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
});
