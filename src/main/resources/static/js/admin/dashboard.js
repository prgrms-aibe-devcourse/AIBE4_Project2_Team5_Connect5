// /static/js/admin/dashboard.js
document.addEventListener('DOMContentLoaded', () => {
    // --- 상태 변수 ---
    let currentUserPage = 0;
    let currentReportPage = 0;
    let currentPenaltyPage = 0;
    const pageSize = 10;
    let currentReportId = null;

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

    // 제재 이력 검색
    document.getElementById('searchPenaltyBtn').addEventListener('click', () => {
        const userId = document.getElementById('searchPenaltyUserId').value.trim();
        if (userId) {
            loadUserPenalties(userId);
        } else {
            currentPenaltyPage = 0;
            loadPenalties();
        }
    });

    //  모달 관련 이벤트 리스너
    document.getElementById('closeReportModal').addEventListener('click', closeReportModal);
    document.getElementById('modalCancelBtn').addEventListener('click', closeReportModal);
    document.getElementById('modalSubmitBtn').addEventListener('click', handleReportSubmit);


    // 탭 전환
    ['users', 'reports', 'penalties'].forEach(tabName => {
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
            <td class="px-6 py-4"><div class="text-sm text-gray-900">${getReasonText(report.reason)}</div><div class="text-xs text-gray-500 truncate max-w-xs">${escapeHtml(report.description)}</div></td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${formatDate(report.createdAt)}</td>
            <td class="px-6 py-4 whitespace-nowrap">${getStatusBadge(report.status, 'report')}</td>
            <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <button onclick="openReportDetailModal(${report.reportId})" class="text-indigo-600 hover:text-indigo-900">상세보기</button>
            </td>
        `;
            tbody.appendChild(tr);
        });
    }

    // 신고 상세 모달 열기
    window.openReportDetailModal = async function(reportId) {
        try {
            const response = await fetch(`/api/admin/reports/${reportId}`, { credentials: 'same-origin' });
            if (!response.ok) throw new Error(`Failed to load report detail: ${response.statusText}`);

            const report = await response.json();
            currentReportId = reportId;

            // 모달에 데이터 채우기
            document.getElementById('modalReporterImage').src = report.reporterProfileImage || '/images/profile/base.png';
            document.getElementById('modalReporterNickname').textContent = report.reporterNickname;
            document.getElementById('modalReporterUserId').textContent = report.reporterUserId.substring(0, 8) + '...';

            document.getElementById('modalReportedImage').src = report.reportedUserProfileImage || '/images/profile/base.png';
            document.getElementById('modalReportedNickname').textContent = report.reportedUserNickname;
            document.getElementById('modalReportedUserId').textContent = report.reportedUserId.substring(0, 8) + '...';

            document.getElementById('modalReportReason').textContent = getReasonText(report.reason);
            document.getElementById('modalReportDescription').textContent = report.description || '상세 설명 없음';
            document.getElementById('modalReportCreatedAt').textContent = formatDateTime(report.createdAt);

            // 이미 처리된 신고인지 확인
            if (report.status === 'RESOLVED' || report.status === 'REJECTED') {
                document.getElementById('modalActionSection').classList.add('hidden');
                document.getElementById('modalAlreadyResolved').classList.remove('hidden');
                document.getElementById('modalResolvedAction').textContent = `처리 완료: ${getActionText(report.action)}`;
            } else {
                document.getElementById('modalActionSection').classList.remove('hidden');
                document.getElementById('modalAlreadyResolved').classList.add('hidden');
                document.getElementById('modalReportAction').value = '';
            }

            // 모달 표시
            document.getElementById('reportDetailModal').classList.remove('hidden');
            document.getElementById('reportDetailModal').classList.add('flex');

            // 신고 목록 새로고침 (PENDING → UNDER_REVIEW 변경 반영)
            loadReports();
        } catch (error) {
            console.error('Error loading report detail:', error);
            alert('신고 상세 정보를 불러올 수 없습니다.');
        }
    };

    // 모달 닫기
    function closeReportModal() {
        document.getElementById('reportDetailModal').classList.add('hidden');
        document.getElementById('reportDetailModal').classList.remove('flex');
        currentReportId = null;
    }

    // 신고 처리 제출
    async function handleReportSubmit() {
        const action = document.getElementById('modalReportAction').value;

        if (!action) {
            alert('처리 조치를 선택해주세요.');
            return;
        }

        if (!currentReportId) {
            alert('처리할 신고가 선택되지 않았습니다.');
            return;
        }

        const confirmMessage = getConfirmMessage(action);
        if (!confirm(confirmMessage)) {
            return;
        }

        try {
            const response = await fetch(`/api/admin/reports/${currentReportId}/resolve?action=${action}`, {
                method: 'PATCH',
                credentials: 'same-origin'
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || '신고 처리에 실패했습니다.');
            }

            alert('신고 처리가 완료되었습니다.');
            closeReportModal();
            loadReports(); // 목록 새로고침
        } catch (error) {
            console.error('Error resolving report:', error);
            alert('신고 처리 중 오류가 발생했습니다: ' + error.message);
        }
    }

    function getConfirmMessage(action) {
        const messages = {
            'NONE': '조치 없이 신고를 반려하시겠습니까?',
            'SUSPEND_1D': '피신고자를 1일 정지 처리하시겠습니까?',
            'SUSPEND_7D': '피신고자를 7일 정지 처리하시겠습니까?',
            'BAN': '피신고자를 영구 차단 처리하시겠습니까? 이 작업은 되돌릴 수 없습니다.'
        };
        return messages[action] || '이 신고를 처리하시겠습니까?';
    }

    function getActionText(action) {
        const actions = {
            'NONE': '조치 없음 (반려)',
            'SUSPEND_1D': '1일 정지',
            'SUSPEND_7D': '7일 정지',
            'BAN': '영구 차단'
        };
        return actions[action] || action;
    }

    function getReasonText(reason) {
        const reasons = {
            'FRAUD_SUSPECT': '사기 의심',
            'ITEM_NOT_AS_DESCRIBED': '설명/사진 불일치',
            'ABUSIVE_LANGUAGE': '욕설/비매너',
            'SPAM_AD': '광고/도배',
            'ILLEGAL_ITEM': '불법/금지 품목',
            'COUNTERFEIT': '가품 의심',
            'PERSONAL_INFO': '개인정보 노출',
            'OTHER': '기타'
        };
        return reasons[reason] || reason;
    }

    // --- 제재 이력 관리 기능 ---
    async function loadPenalties() {
        const type = document.getElementById('filterPenaltyType').value;
        const params = new URLSearchParams({ page: currentPenaltyPage, size: pageSize });
        if (type) params.append('type', type);

        try {
            const response = await fetch(`/api/admin/penalties?${params}`, { credentials: 'same-origin' });
            if (!response.ok) throw new Error(`Failed to load penalties: ${response.statusText}`);

            const data = await response.json();
            renderPenaltyTable(data.content);
            renderPagination('penaltyPagination', data.pageInfo, (page) => {
                currentPenaltyPage = page;
                loadPenalties();
            });
        } catch (error) {
            console.error('Error loading penalties:', error);
            document.getElementById('penaltyList').innerHTML = `<tr><td colspan="6" class="text-center py-4">제재 이력을 불러올 수 없습니다.</td></tr>`;
        }
    }

    async function loadUserPenalties(userIdOrNickname) {
        try {
            // userId로 시도
            const response = await fetch(`/api/admin/users/${userIdOrNickname}/penalties`, { credentials: 'same-origin' });

            if (!response.ok) {
                if (response.status === 400) {
                    alert('올바른 사용자 ID(UUID)를 입력해주세요.');
                } else {
                    throw new Error(`Failed to load user penalties: ${response.statusText}`);
                }
                return;
            }

            const penalties = await response.json();
            renderPenaltyTable(penalties);

            // 특정 유저 조회 시 페이지네이션 숨김
            document.getElementById('penaltyPagination').innerHTML = '';
        } catch (error) {
            console.error('Error loading user penalties:', error);
            document.getElementById('penaltyList').innerHTML = `<tr><td colspan="6" class="text-center py-4">해당 사용자의 제재 이력을 찾을 수 없습니다.</td></tr>`;
        }
    }

    function renderPenaltyTable(penalties) {
        const tbody = document.getElementById('penaltyList');
        tbody.innerHTML = '';

        if (!penalties || penalties.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4">제재 이력이 없습니다.</td></tr>`;
            return;
        }

        penalties.forEach(penalty => {
            const tr = document.createElement('tr');
            tr.className = 'hover:bg-gray-50';
            tr.innerHTML = `
                <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center">
                        <div class="h-10 w-10 flex-shrink-0">
                            <img class="h-10 w-10 rounded-full bg-gray-200" 
                                 src="${penalty.profileImageUrl || '/images/profile/base.png'}" 
                                 alt="프로필">
                        </div>
                        <div class="ml-4">
                            <div class="text-sm font-medium text-gray-900">${escapeHtml(penalty.nickname)}</div>
                            <div class="text-xs text-gray-500">${penalty.userId.substring(0, 8)}...</div>
                        </div>
                    </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${getPenaltyTypeBadge(penalty.type)}
                </td>
                <td class="px-6 py-4">
                    <div class="text-sm text-gray-900">${escapeHtml(penalty.reason)}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    ${formatDateTime(penalty.startedAt)}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    ${penalty.expiresAt ? formatDateTime(penalty.expiresAt) : '<span class="text-red-600 font-semibold">영구</span>'}
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                    ${penalty.isActive
                ? '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">활성</span>'
                : '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-800">만료</span>'}
                </td>
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
        ['users', 'reports', 'penalties'].forEach(name => {
            const section = document.getElementById('section-' + name);
            if (section) section.classList.add('hidden');
            
            const tabBtn = document.getElementById('tab-' + name);
            if (tabBtn) {
                tabBtn.classList.remove('bg-slate-900', 'text-white', 'shadow-sm');
                tabBtn.classList.add('text-gray-600', 'hover:bg-gray-100');
            }
        });

        const activeSection = document.getElementById('section-' + tabName);
        if (activeSection) activeSection.classList.remove('hidden');
        
        const activeTab = document.getElementById('tab-' + tabName);
        if (activeTab) {
            activeTab.classList.remove('text-gray-600', 'hover:bg-gray-100');
            activeTab.classList.add('bg-slate-900', 'text-white', 'shadow-sm');
        }

        if (tabName === 'reports') {
            loadReports();
        } else if (tabName === 'penalties') {
            loadPenalties();
        }
    }

    function getStatusBadge(status, type) {
        const userBadges = {
            ACTIVE: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">ACTIVE</span>',
            SUSPENDED: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-yellow-100 text-yellow-800">SUSPENDED</span>',
            BANNED: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">BANNED</span>'
        };
        const reportBadges = {
            PENDING: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-yellow-100 text-yellow-800">처리 대기</span>',
            UNDER_REVIEW: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">처리중</span>',
            REJECTED: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-black-100 text-red-800">기각</span>',
            RESOLVED: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-800">처리 완료</span>'
        };
        if (type === 'user') return userBadges[status] || status;
        if (type === 'report') return reportBadges[status] || status;
        return status;
    }

    function getPenaltyTypeBadge(type) {
        const badges = {
            SUSPENDED: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-yellow-100 text-yellow-800">정지</span>',
            BANNED: '<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">차단</span>'
        };
        return badges[type] || type;
    }

    function formatDate(dateString) {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleDateString('ko-KR');
    }

    function formatDateTime(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR') + ' ' + date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
    }

    function escapeHtml(text) {
        if (text === null || text === undefined) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    window.manageUser = (userId) => alert(`사용자 관리 모달을 엽니다: ${userId}`);
});
