let timerInterval; // 타이머 인터벌 변수
let selectedImageFile = null; // 선택된 이미지 파일

async function apiCall(url, options = {}) {
    try {
        const response = await fetch(url, options);
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `HTTP ${response.status}`);
        }
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.indexOf("application/json") !== -1) {
            return await response.json();
        }
        return {};
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

function updatePhoneAuthUI(isVerified) {
    const phoneInput = document.getElementById('phoneNumber');
    const sendBtn = document.getElementById('sendVerificationBtn');
    const verificationSection = document.getElementById('verificationCodeSection');
    const statusBadge = document.getElementById('phoneAuthStatus');

    if (isVerified) {
        phoneInput.readOnly = true;
        phoneInput.classList.add('bg-gray-100', 'cursor-not-allowed');
        sendBtn.disabled = true;
        sendBtn.classList.add('bg-gray-400', 'cursor-not-allowed', 'hover:bg-gray-400');
        sendBtn.classList.remove('bg-slate-900', 'hover:bg-slate-800');
        verificationSection.classList.add('hidden');
        statusBadge.textContent = '인증 완료';
        statusBadge.className = 'text-xs font-medium text-green-600';
    } else {
        phoneInput.readOnly = false;
        phoneInput.classList.remove('bg-gray-100', 'cursor-not-allowed');
        sendBtn.disabled = false;
        sendBtn.classList.remove('bg-gray-400', 'cursor-not-allowed', 'hover:bg-gray-400');
        sendBtn.classList.add('bg-slate-900', 'hover:bg-slate-800');
        statusBadge.textContent = '미인증';
        statusBadge.className = 'text-xs font-medium text-red-600';
    }
}

// 타이머 시작 함수 (duration: 초 단위)
function startTimer(duration, display) {
    let timer = duration, minutes, seconds;

    // 기존 타이머가 있다면 제거
    if (timerInterval) clearInterval(timerInterval);

    function updateDisplay() {
        minutes = parseInt(timer / 60, 10);
        seconds = parseInt(timer % 60, 10);

        minutes = minutes < 10 ? "0" + minutes : minutes;
        seconds = seconds < 10 ? "0" + seconds : seconds;

        display.textContent = "남은 시간: " + minutes + ":" + seconds;

        if (--timer < 0) {
            clearInterval(timerInterval);
            display.textContent = "인증 시간이 만료되었습니다.";
            display.classList.add('text-red-600');
        } else {
            display.classList.remove('text-red-600');
        }
    }

    updateDisplay(); // 즉시 한 번 실행
    timerInterval = setInterval(updateDisplay, 1000);
}

async function loadProfile() {
    // 이미 SSR로 데이터가 로드된 경우 API 호출 스킵
    if (currentProfile) {
        return;
    }

    const loadingSpinner = document.getElementById('loadingSpinner');
    const profileCard = document.getElementById('profileCard');
    const profileForm = document.getElementById('profileForm');
    const errorMessage = document.getElementById('errorMessage');

    try {
        const profile = await apiCall('/api/users/me');
        currentProfile = profile;

        const profileImage = document.getElementById('profileImage');
        if (profile.profileImageUrl) {
            profileImage.src = profile.profileImageUrl;
        } else {
            profileImage.src = '/images/profile/base.png';
        }

        document.getElementById('profileNickname').textContent = profile.nickname;
        document.getElementById('tradeCount').textContent = profile.tradeCount;
        document.getElementById('ratingAvg').textContent = profile.ratingAvg.toFixed(2);
        document.getElementById('reportCount').textContent = profile.reportCount;

        document.getElementById('name').value = profile.name;
        document.getElementById('nickname').value = profile.nickname;
        document.getElementById('phoneNumber').value = profile.phoneNumber || '';

        updatePhoneAuthUI(profile.phoneVerified);

        loadingSpinner.classList.add('hidden');
        profileCard.classList.remove('hidden');
        profileForm.classList.remove('hidden');
    } catch (error) {
        console.error('프로필 로드 실패:', error);
        loadingSpinner.classList.add('hidden');
        errorMessage.textContent = '프로필을 불러올 수 없습니다. 다시 로그인해주세요.';
        errorMessage.classList.remove('hidden');
    }
}

// 프로필 이미지 선택 시 미리보기
document.getElementById('profileImageInput').addEventListener('change', function(e) {
    const file = e.target.files[0];
    if (file) {
        // 용량 체크 (2MB)
        if (file.size > 2 * 1024 * 1024) {
            alert('이미지 용량은 2MB 이하여야 합니다.');
            this.value = ''; // 초기화
            return;
        }

        // 확장자 체크
        const allowedExtensions = ['jpg', 'jpeg', 'png', 'webp'];
        const extension = file.name.split('.').pop().toLowerCase();
        if (!allowedExtensions.includes(extension)) {
            alert('jpg, jpeg, png, webp 확장자만 가능합니다.');
            this.value = ''; // 초기화
            return;
        }

        // 이미지 크기(가로/세로) 체크
        const img = new Image();
        const objectUrl = URL.createObjectURL(file);

        img.onload = function() {
            URL.revokeObjectURL(objectUrl); // 메모리 해제

            if (this.width > 400 || this.height > 400) {
                alert('이미지 크기는 400x400 이하여야 합니다.');
                document.getElementById('profileImageInput').value = ''; // 초기화
                return;
            }

            // 모든 검증 통과 시 미리보기 적용
            selectedImageFile = file;
            const reader = new FileReader();
            reader.onload = function(e) {
                document.getElementById('profileImage').src = e.target.result;
            }
            reader.readAsDataURL(file);
        };

        img.onerror = function() {
            URL.revokeObjectURL(objectUrl);
            alert('이미지 파일을 읽을 수 없습니다.');
            document.getElementById('profileImageInput').value = '';
        };

        img.src = objectUrl;
    }
});

// 닉네임 중복 확인
document.getElementById('checkNicknameBtn').addEventListener('click', async () => {
    const nickname = document.getElementById('nickname').value.trim();
    const nicknameError = document.getElementById('nicknameError');
    const nicknameSuccess = document.getElementById('nicknameSuccess');

    nicknameError.classList.add('hidden');
    nicknameSuccess.classList.add('hidden');

    if (!nickname) {
        nicknameError.textContent = '닉네임을 입력해주세요';
        nicknameError.classList.remove('hidden');
        return;
    }

    if (currentProfile && nickname === currentProfile.nickname) {
        nicknameSuccess.classList.remove('hidden');
        return;
    }

    try {
        const response = await apiCall('/api/users/checkNickname', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ nickname })
        });

        if (response.available) {
            nicknameSuccess.classList.remove('hidden');
        } else {
            nicknameError.textContent = '이미 사용 중인 닉네임입니다';
            nicknameError.classList.remove('hidden');
        }
    } catch (error) {
        nicknameError.textContent = '중복 확인 중 오류가 발생했습니다';
        nicknameError.classList.remove('hidden');
    }
});

// 인증 코드 발송
document.getElementById('sendVerificationBtn').addEventListener('click', async () => {
    const phoneNumber = document.getElementById('phoneNumber').value.trim();
    const phoneError = document.getElementById('phoneError');
    const verificationSection = document.getElementById('verificationCodeSection');
    const verificationTimer = document.getElementById('verificationTimer');

    phoneError.classList.add('hidden');

    if (!phoneNumber) {
        phoneError.textContent = '휴대폰 번호를 입력해주세요';
        phoneError.classList.remove('hidden');
        return;
    }

    if (!/^\d{10,11}$/.test(phoneNumber)) {
        phoneError.textContent = '유효한 휴대폰 번호를 입력해주세요';
        phoneError.classList.remove('hidden');
        return;
    }

    try {
        await apiCall(`/api/users/sendPhoneVerification?phoneNumber=${phoneNumber}`, {
            method: 'POST'
        });
        alert('인증 코드가 발송되었습니다');
        verificationSection.classList.remove('hidden');

        // 타이머 시작 (5분 = 300초)
        startTimer(300, verificationTimer);

    } catch (error) {
        phoneError.textContent = '인증 코드 발송 중 오류가 발생했습니다';
        phoneError.classList.remove('hidden');
    }
});

// 인증 코드 검증
document.getElementById('confirmVerificationBtn').addEventListener('click', async () => {
    const phoneNumber = document.getElementById('phoneNumber').value.trim();
    const verificationCode = document.getElementById('verificationCode').value.trim();
    const verificationError = document.getElementById('verificationError');

    verificationError.classList.add('hidden');

    if (!verificationCode) {
        verificationError.textContent = '인증 코드를 입력해주세요';
        verificationError.classList.remove('hidden');
        return;
    }

    try {
        const response = await apiCall('/api/users/verifyPhone', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ phoneNumber, verificationCode })
        });

        if (response.verified) {
            if (timerInterval) clearInterval(timerInterval); // 타이머 정지
            alert('휴대폰 번호 인증이 완료되었습니다. 페이지를 새로고침합니다.');
            location.reload();
        } else {
            verificationError.textContent = response.message || '인증에 실패했습니다';
            verificationError.classList.remove('hidden');
        }
    } catch (error) {
        verificationError.textContent = '인증 확인 중 오류가 발생했습니다';
        verificationError.classList.remove('hidden');
    }
});

// 프로필 저장
document.getElementById('profileForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const name = document.getElementById('name').value.trim();
    const nickname = document.getElementById('nickname').value.trim();
    const phoneNumber = currentProfile.phoneVerified ? currentProfile.phoneNumber : (document.getElementById('phoneNumber').value.trim() || null);

    const nameError = document.getElementById('nameError');
    const nicknameError = document.getElementById('nicknameError');

    nameError.classList.add('hidden');
    nicknameError.classList.add('hidden');

    if (!name) {
        nameError.textContent = '이름을 입력해주세요';
        nameError.classList.remove('hidden');
        return;
    }

    if (!nickname) {
        nicknameError.textContent = '닉네임을 입력해주세요';
        nicknameError.classList.remove('hidden');
        return;
    }

    try {
        const formData = new FormData();
        const requestData = {
            name: name,
            nickname: nickname,
            phoneNumber: phoneNumber
        };

        formData.append('data', new Blob([JSON.stringify(requestData)], { type: 'application/json' }));

        if (selectedImageFile) {
            formData.append('image', selectedImageFile);
        }

        const updatedProfile = await apiCall('/api/users/me', {
            method: 'PUT',
            body: formData
        });

        alert('프로필이 저장되었습니다');

        // 네비게이션 바 프로필 이미지 업데이트
        const navProfileImg = document.querySelector('#profileButton img');
        if (navProfileImg && updatedProfile.profileImageUrl) {
            navProfileImg.src = updatedProfile.profileImageUrl;
        }

        // 현재 페이지 프로필 정보 업데이트
        currentProfile = updatedProfile;
        document.getElementById('profileNickname').textContent = updatedProfile.nickname;
        if (updatedProfile.profileImageUrl) {
            document.getElementById('profileImage').src = updatedProfile.profileImageUrl;
        }
        selectedImageFile = null; // 이미지 파일 선택 상태 초기화

    } catch (error) {
        alert('프로필 저장 중 오류가 발생했습니다: ' + error.message);
    }
});

// 페이지 로드 시 실행
loadProfile();
