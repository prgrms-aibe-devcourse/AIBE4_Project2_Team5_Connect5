(function () {
  const form = document.getElementById("auctionFilterForm");
  if (!form) return;

  const sizeSelect = document.getElementById("sizeSelect");
  const sortInput = document.getElementById("sortInput");
  const viewInput = document.getElementById("viewInput");
  const resetBtn = document.getElementById("resetFiltersBtn");

  const safeSubmit = () => {
    // SSR: GET form submit로 쿼리스트링 반영
    form.submit();
  };

  if (sizeSelect) {
    sizeSelect.addEventListener("change", () => {
      safeSubmit();
    });
  }

  const viewButtons = Array.from(document.querySelectorAll(".filter-view-btn"));
  const applyViewActive = (value) => {
    viewButtons.forEach((btn) => {
      const isActive = btn.dataset.view === value;
      btn.classList.toggle("bg-slate-900", isActive);
      btn.classList.toggle("text-white", isActive);
      btn.classList.toggle("bg-white", !isActive);
      btn.classList.toggle("text-slate-700", !isActive);
    });
  };

  const initialView = (viewInput && viewInput.value) ? viewInput.value : "grid";
  applyViewActive(initialView);

  viewButtons.forEach((btn) => {
    btn.addEventListener("click", () => {
      const v = btn.dataset.view || "";
      if (viewInput) viewInput.value = v;
      applyViewActive(v);
      safeSubmit();
    });
  });

  // ---------- Sort dropdown ----------
  const dropdownRoot = document.querySelector('[data-dropdown="sort"]');
  if (dropdownRoot) {
    const toggle = dropdownRoot.querySelector(".dropdown-toggle");
    const menu = dropdownRoot.querySelector(".dropdown-menu");
    const label = dropdownRoot.querySelector(".dropdown-label");
    const items = Array.from(dropdownRoot.querySelectorAll(".dropdown-item"));

    const close = () => {
      if (!menu) return;
      menu.classList.add("hidden");
      toggle?.setAttribute("aria-expanded", "false");
    };

    const open = () => {
      if (!menu) return;
      menu.classList.remove("hidden");
      toggle?.setAttribute("aria-expanded", "true");
    };

    const isOpen = () => menu && !menu.classList.contains("hidden");

    const initSort = sortInput?.value || "";
    if (initSort && items.length) {
      const matched = items.find((it) => it.dataset.value === initSort);
      if (matched && label) label.textContent = matched.textContent.trim();
    } else {
      if (label && !label.textContent.trim()) label.textContent = "정렬";
    }

    toggle?.addEventListener("click", (e) => {
      e.preventDefault();
      e.stopPropagation();
      if (isOpen()) close();
      else open();
    });

    items.forEach((it) => {
      it.addEventListener("click", (e) => {
        e.preventDefault();
        const v = it.dataset.value || "";
        if (sortInput) sortInput.value = v;
        if (label) label.textContent = it.textContent.trim();
        close();
        safeSubmit();
      });
    });

    // 바깥 클릭/ESC로 닫기
    document.addEventListener("click", (e) => {
      if (!dropdownRoot.contains(e.target)) close();
    });

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") close();
    });
  }

  if (resetBtn) {
    resetBtn.addEventListener("click", () => {
      // q, size, sort, view 초기화 (필요하면 유지할 항목만 남기세요)
      const qInput = document.getElementById("qInput");
      if (qInput) qInput.value = "";

      if (sizeSelect) sizeSelect.value = "";
      if (sortInput) sortInput.value = "";
      if (viewInput) viewInput.value = "grid";

      safeSubmit();
    });
  }
})();
