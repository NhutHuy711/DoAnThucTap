document.addEventListener("DOMContentLoaded", function () {
    const brandItems = document.querySelectorAll(".brand-item a"); // chọn <a> thay vì <li>
    const categoryLists = document.querySelectorAll(".category-list");

    function showCategories(index) {
        categoryLists.forEach(list => {
        list.style.display = (list.getAttribute("data-index") === index) ? "block" : "none";
    });

    brandItems.forEach(item => {
        item.parentElement.classList.toggle("active", item.getAttribute("data-index") === index);
    });
}

    // Hover vào link brand
    brandItems.forEach(item => {
        item.addEventListener("mouseenter", function () {
            showCategories(this.getAttribute("data-index"));
        });
    });

    // Hiển thị mặc định brand đầu tiên
    if (brandItems.length > 0) {
        showCategories(brandItems[0].getAttribute("data-index"));
    }
});
