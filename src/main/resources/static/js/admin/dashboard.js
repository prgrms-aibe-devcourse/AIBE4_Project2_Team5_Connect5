// /static/js/admin/dashboard.js
document.addEventListener('DOMContentLoaded', () => {
    // --- 상태 변수 ---
    let currentUserPage = 0;
    let currentReportPage = 0;
    const pageSize = 10;
    let userGrowthChart = null;
    let transactionVolumeChart = null;

    // --- 초기화 ---
    loadUsers(); // 기본으로 사용자 목록 로드

    // --- 이벤트 리스너 ---
    // 사용자 검색
    document.getElementById('searchBtn').addEventListener('click', () => {
        currentUserPage = 0;
        loadUsers();
    });

    // 신고 검색
    document.getElementById('searchReportBtn').addEventListener('click', () => {
        currentReportPage = 0;
        loadReports();
    });

    // 탭 전환
    ['users', 'reports', 'penalties', 'stats'].forEach(tabName => {
        const tabButton = document.getElementById(`tab-${tabName}`);
        if (tabButton) {
            tabButton.addEventListener('click', () => switchTab(tabName));
        }
    });

    // --- 사용자 관리 기능 ---
    async function loadUsers() {
        const name = document.getElementById('searchName').value;
        const email = document.getElementById('searchEmail').value;
        const status = document.getElementById('filterStatus').value;

        const params = new URLSearchParams({ page: currentUserPage, size: pageSize });
        if (name) params.append('name', name);
        if (email) params.append('email', email);
        if (status) params.append('status', status);

        try {
            const response = await fetch(`/api/admin/users?${params}`, { credentials: 'same-origin' });
            if (!response.ok) throw new Error(`Failed to load users: ${response.statusText}`);
            
            const data = await response.json();
            renderUserTable(data.content);
            renderPagination('pagination', data.pageInfo, (page) => {
                currentUserPage = page;
                loadUsers();
            });
        } catch (error) {
            console.error('Error loading users:', error);
            document.getElementById('userList').innerHTML = `<tr><td colspan="5" class="text-center py-4">사용자 정보를 불러올 수 없습니다.</td></tr>`;
        }
    }

    function renderUserTable(users) {
        const tbody = document.getElementById('userList');
        tbody.innerHTML = '';
        if (!users || users.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center py-4">검색 결과가 없습니다.</td></tr>`;
            return;
        }
        users.forEach(user => {
            const tr = document.createElement('tr');
            tr.className = 'hover:bg-gray-50';
            tr.innerHTML = `
                <td class="px-6 py-4 whitespace-nowrap"><div class="flex items-center"><div class="h-10 w-10 flex-shrink-0"><img class="h-10 w-10 rounded-full bg-gray-200" src="${user.profileImageUrl || '/images/profile/base.png'}" alt="프로필 이미지"></div><div class="ml-4"><div class="text-sm font-medium text-gray-900">${escapeHtml(user.nickname)}</div><div class="text-sm text-gray-500">${user.userId.substring(0, 8)}...</div></div></div></td>
                <td class="px-6 py-4 whitespace-nowrap"><div class="text-sm text-gray-900">${escapeHtml(user.email)}</div><div class="text-sm text-gray-500">${user.phone || '-'}</div></td>
                <td class="px-6 py-4 whitespace-nowrap">${getStatusBadge(user.status, 'user')}</td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${formatDate(user.createdAt)}</td>
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium"><button onclick="manageUser('${user.userId}')" class="text-indigo-600 hover:text-indigo-900 font-medium">관리</button></td>
            `;
            tbody.appendChild(tr);
        });
    }

    // --- 신고 관리 기능 ---
    async function loadReports() {
        const status = document.getElementById('filterReportStatus').value;
        const params = new URLSearchParams({ page: currentReportPage, size: pageSize });
        if (status) params.append('status', status);

        try {
            const response = await fetch(`/api/admin/reports?${params}`, { credentials: 'same-origin' });
            if (!response.ok) throw new Error(`Failed to load reports: ${response.statusText}`);

            const data = await response.json();
            renderReportTable(data.content);
            renderPagination('reportPagination', data.pageInfo, (page) => {
                currentReportPage = page;
                loadReports();
            });
        } catch (error) {
            console.error('Error loading reports:', error);
            document.getElementById('reportList').innerHTML = `<tr><td colspan="6" class="text-center py-4">신고 정보를 불러올 수 없습니다.</td></tr>`;
        }
    }

    function renderReportTable(reports) {
        const tbody = document.getElementById('reportList');
        tbody.innerHTML = '';
        if (!reports || reports.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4">검색 결과가 없습니다.</td></tr>`;
            return;
        }
        reports.forEach(report => {
            const tr = document.createElement('tr');
            tr.className = 'hover:bg-gray-50';
            tr.innerHTML = `
                <td class="px-6 py-4 whitespace-nowrap"><div class="text-sm font-medium text-gray-900">${escapeHtml(report.reportedUserNickname)}</div><div class="text-xs text-gray-500">${report.reportedUserId.substring(0,8)}...</div></td>
                <td class="px-6 py-4 whitespace-nowrap"><div class="text-sm font-medium text-gray-900">${escapeHtml(report.reporterNickname)}</div><div class="text-xs text-gray-500">${report.reporterUserId.substring(0,8)}...</div></td>
                <td class="px-6 py-4"><div class="text-sm text-gray-900">${report.reason}</div><div class="text-xs text-gray-500 truncate max-w-xs">${escapeHtml(report.description)}</div></td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${formatDate(report.createdAt)}</td>
                <td class="px-6 py-4 whitespace-nowrap">${getStatusBadge(report.status, 'report')}</td>
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium"><button onclick="manageReport(${report.reportId})" class="text-indigo-600 hover:text-indigo-900">상세보기</button></td>
            `;
            tbody.appendChild(tr);
        });
    }

    // --- 공용 및 유틸리티 함수 ---
    function renderPagination(containerId, pageInfo, onPageClick) {
        const paginationContainer = document.getElementById(containerId);
        paginationContainer.innerHTML = '';
        if (!pageInfo || pageInfo.totalElements === 0) return;

        const { currentPage: pageNum, totalPages, totalElements, size, first, last } = pageInfo;
        const wrapper = document.createElement('div');
        wrapper.className = 'flex items-center justify-between';

        const resultTextContainer = document.createElement('div');
        resultTextContainer.innerHTML = `<p class="text-sm text-gray-700">Showing <span class="font-medium">${pageNum * size + 1}</span> to <span class="font-medium">${Math.min((pageNum + 1) * size, totalElements)}</span> of <span class="font-medium">${totalElements}</span> results</p>`;
        
        const navContainer = document.createElement('div');
        const nav = document.createElement('nav');
        nav.className = 'relative z-0 inline-flex rounded-md shadow-sm -space-x-px';
        nav.setAttribute('aria-label', 'Pagination');

        const prevButton = document.createElement('button');
        prevButton.className = 'relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50';
        prevButton.innerHTML = `<span class="sr-only">Previous</span><svg class="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clip-rule="evenodd" /></svg>`;
        if (first) {
            prevButton.disabled = true;
            prevButton.classList.add('cursor-not-allowed', 'opacity-50');
        } else {
            prevButton.addEventListener('click', () => onPageClick(pageNum - 1));
        }
        nav.appendChild(prevButton);

        const pageGroupSize = 5;
        const startPage = Math.floor(pageNum / pageGroupSize) * pageGroupSize;
        const endPage = Math.min(startPage + pageGroupSize, totalPages);
        for (let i = startPage; i < endPage; i++) {
            const pageButton = document.createElement('button');
            pageButton.innerText = i + 1;
            if (i === pageNum) {
                pageButton.className = 'z-10 bg-indigo-50 border-indigo-500 text-indigo-600 relative inline-flex items-center px-4 py-2 border text-sm font-medium';
            } else {
                pageButton.className = 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50 relative inline-flex items-center px-4 py-2 border text-sm font-medium';
                pageButton.addEventListener('click', () => onPageClick(i));
            }
            nav.appendChild(pageButton);
        }

        const nextButton = document.createElement('button');
        nextButton.className = 'relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50';
        nextButton.innerHTML = `<span class="sr-only">Next</span><svg class="h-5 w-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true"><path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd" /></svg>`;
        if (last) {
            nextButton.disabled = true;
            nextButton.classList.add('cursor-not-allowed', 'opacity-50');
        } else {
            nextButton.addEventListener('click', () => onPageClick(pageNum + 1));
        }
        nav.appendChild(nextButton);
        navContainer.appendChild(nav);

        wrapper.appendChild(resultTextContainer);
        wrapper.appendChild(navContainer);
        paginationContainer.appendChild(wrapper);
    }

    function switchTab(tabName) {
        ['users', 'reports', 'penalties', 'stats'].forEach(name => {
            document.getElementById('section-' + name).classList.add('hidden');
            const tabBtn = document.getElementById('tab-' + name);
            tabBtn.classList.remove('bg-slate-900', 'text-white', 'shadow-sm');
            tabBtn.classList.add('text-gray-600', 'hover:bg-gray-100');
        });

        const activeSection = document.getElementById('section-' + tabName);
        activeSection.classList.remove('hidden');
        const activeTab = document.getElementById('tab-' + tabName);
        activeTab.classList.remove('text-gray-600', 'hover:bg-gray-100');
        activeTab.classList.add('bg-slate-900', 'text-white', 'shadow-sm');

        if (tabName === 'reports') {
            loadReports();
        } else if (tabName === 'stats') {
            renderCharts();
        }
    }

    function renderCharts() {
        if (userGrowthChart) return;
        const commonOptions = { responsive: true, maintainAspectRatio: false };
        const ctxUser = document.getElementById('userGrowthChart').getContext('2d');
        userGrowthChart = new Chart(ctxUser, { type: 'line', data: { labels: ['5월', '6월', '7월', '8월', '9월', '10월'], datasets: [{ label: '신규 가입자 수', data: [120, 150, 180, 220, 300, 450], borderColor: 'rgb(75, 192, 192)', tension: 0.1 }] }, options: commonOptions });
        const ctxTransaction = document.getElementById('transactionVolumeChart').getContext('2d');
        transactionVolumeChart = new Chart(ctxTransaction, { type: 'bar', data: { labels: ['5월', '6월', '7월', '8월', '9월', '10월'], datasets: [{ label: '거래 완료 건수', data: [50, 75, 90, 120, 160, 210], backgroundColor: 'rgba(54, 162, 235, 0.6)' }] }, options: { ...commonOptions, scales: { y: { beginAtZero: true } } } });
    }

    function getStatusBadge(status, type) {
        const userBadges = {
            ACTIVE: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">ACTIVE</span>',
            SUSPENDED: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-yellow-100 text-yellow-800">SUSPENDED</span>',
            BANNED: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">BANNED</span>'
        };
        const reportBadges = {
            PENDING: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-yellow-100 text-yellow-800">처리 대기</span>',
            RESOLVED: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-800">처리 완료</span>'
        };
        if (type === 'user') return userBadges[status] || status;
        if (type === 'report') return reportBadges[status] || status;
        return status;
    }

    function formatDate(dateString) {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleDateString('ko-KR');
    }

    function escapeHtml(text) {
        if (text === null || text === undefined) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    window.manageUser = (userId) => alert(`사용자 관리 모달을 엽니다: ${userId}`);
    window.manageReport = (reportId) => alert(`신고 상세 보기 모달을 엽니다: ${reportId}`);
});
