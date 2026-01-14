document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    const errorMessage = urlParams.get('errorMessage');

    if (errorMessage) {
        alert(errorMessage);
    }
});
