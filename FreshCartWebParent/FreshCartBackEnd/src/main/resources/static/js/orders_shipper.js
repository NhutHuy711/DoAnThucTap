var iconNames = {
    'PICKED': 'fa-people-carry',
    'SHIPPING': 'fa-shipping-fast',
    'DELIVERED': 'fa-box-open',
    'RETURNED': 'fa-undo'
};

let confirmModal; // Bootstrap 5 modal instance
let $confirmText, $yesButton, $noButton;

$(function () {
    const modalEl = document.getElementById('confirmModal');
    confirmModal = new bootstrap.Modal(modalEl);

    $confirmText = $("#confirmText");
    $yesButton   = $("#yesButton");
    $noButton    = $("#noButton");

    // Mở chi tiết đơn (giữ nguyên phần của bạn)
    document.querySelectorAll(".linkOrderDetail").forEach(link => {
        link.addEventListener("click", function (e) {
            e.preventDefault();
            const linkDetailURL = this.getAttribute("href");
            const orderDetailModal = new bootstrap.Modal(document.getElementById('orderDetailModal'));
            const modalContent = document.querySelector("#orderDetailModal .modal-content");
            modalContent.innerHTML = "";
            fetch(linkDetailURL).then(r => r.text()).then(html => {
                modalContent.innerHTML = html; orderDetailModal.show();
            }).catch(() => {
                modalContent.innerHTML = "<p>Error loading order details. Please try again.</p>";
                orderDetailModal.show();
            });
        });
    });

    // Click icon cập nhật trạng thái
    $(document).on("click", ".linkUpdateStatus", function (e) {
        e.preventDefault();
        const $link    = $(this);
        const orderId  = $link.data("order-id");
        const current  = ($link.data("current-status") || "").toString().toUpperCase();
        const target   = ($link.data("status") || "").toString().toUpperCase();
        const href     = $link.data("href") || $link.attr("href");

        // Kiểm tra thứ tự hợp lệ trước khi mở confirm
        if (!isAllowedTransition(current, target)) {
            showMessageModal(
                `Cannot transition from ${current || 'NEW'} to ${target}.
            Valid sequence: PICKED → SHIPPING → DELIVERED -> RETURNED.`
            );
            return;
        }

        $yesButton.attr("href", href);
        $confirmText.text(`Are you sure you want to update status of the order ID #${orderId} to ${target}?`);
        $noButton.text("NO").show();
        $yesButton.show();
        confirmModal.show();
    });

    // Nút NO hoặc nút đóng (Bootstrap 5)
    $(document).on("click", "#noButton, .btn-close, [data-bs-dismiss='modal']", function () {
        confirmModal.hide();
    });

    // Nút YES
    $yesButton.off("click").on("click", function (e) {
        e.preventDefault();
        const requestURL = $(this).attr("href");

        $.ajax({
            type: "POST",
            url: requestURL,
            beforeSend: function (xhr) {
                xhr.setRequestHeader(csrfHeaderName, csrfValue);
            }
        }).done(function (response) {
            showMessageModal("Order updated successfully");

            const modalEl  = document.getElementById('confirmModal');
            const instance = bootstrap.Modal.getOrCreateInstance(modalEl);

            // Khi modal đóng => reload để cập nhật toàn bộ icon/link
            const onHidden = () => {
                modalEl.removeEventListener('hidden.bs.modal', onHidden);
                location.reload();
            };
            modalEl.addEventListener('hidden.bs.modal', onHidden);

            // Đóng modal tự động sau 800ms (người dùng vẫn có thể bấm Close ngay)
            setTimeout(() => instance.hide(), 800);
        }).fail(function (err) {
            const msg = err?.responseJSON?.message
                || (err?.status === 409 ? "Invalid status transition" : "Error updating order status");
            showMessageModal(msg);
        });
    });
});

function isAllowedTransition(current, target) {
    // Điều chỉnh cho đúng domain của bạn nếu khác
    const flow = {
        'NEW':       ['PICKED'],
        'PROCESSING':['PICKED'],
        'PICKED':    ['SHIPPING'],
        'SHIPPING':  ['DELIVERED'],
        'DELIVERED': ['RETURNED'],
        'RETURNED':  []
    };
    const allowed = flow[current] || flow['NEW'];
    return allowed.includes(target);
}

function updateStatusIconColor(orderId, status) {
    const $link = $(`#link${status}${orderId}`);
    $link.replaceWith(`<i style="color:#0aad0a" class="fas ${iconNames[status]} fa-2x"></i>`);
}

function showMessageModal(message) {
    $confirmText.text(message);
    $yesButton.hide();
    $noButton.text("Close").show();
    confirmModal.show();

    // Reset lại nút khi đóng
    const modalEl = document.getElementById('confirmModal');
    modalEl.addEventListener('hidden.bs.modal', function handler() {
        $yesButton.show();
        $noButton.text("NO");
        modalEl.removeEventListener('hidden.bs.modal', handler);
    });
}
