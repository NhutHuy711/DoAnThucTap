document.addEventListener('DOMContentLoaded', function () {
    // Fields
    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');

    // Icons
    const toggler = document.getElementById('passwordToggler');
    const confirmToggler = document.getElementById('confirmPasswordToggler');

    // === Show/Hide ===
    function toggle(field, icon) {
        if (!field || !icon) return;
        if (field.type === 'password') {
            field.type = 'text';
            icon.classList.remove('bi-eye-slash');
            icon.classList.add('bi-eye');
        } else {
            field.type = 'password';
            icon.classList.remove('bi-eye');
            icon.classList.add('bi-eye-slash');
        }
    }

    toggler?.addEventListener('click', () => toggle(password, toggler));
    confirmToggler?.addEventListener('click', () => toggle(confirmPassword, confirmToggler));

    // === Validate match ===
    function validateMatch() {
        if (!password || !confirmPassword) return;

        const pass = password.value;
        const conf = confirmPassword.value;

        // Xóa trạng thái khi chưa nhập xác nhận
        if (conf.length === 0) {
            confirmPassword.setCustomValidity('');
            confirmPassword.classList.remove('is-valid', 'is-invalid');
            return;
        }

        if (pass !== conf) {
            confirmPassword.setCustomValidity('Passwords do not match');
            confirmPassword.classList.add('is-invalid');
            confirmPassword.classList.remove('is-valid');
        } else {
            confirmPassword.setCustomValidity('');
            confirmPassword.classList.remove('is-invalid');
            confirmPassword.classList.add('is-valid');
        }
    }

    // Lắng nghe thay đổi để kiểm tra realtime
    password?.addEventListener('input', validateMatch);
    confirmPassword?.addEventListener('input', validateMatch);

    // Expose ra global để HTML inline oninput gọi được
    window.checkPasswordMatch = function () {
        validateMatch();
    };
});
