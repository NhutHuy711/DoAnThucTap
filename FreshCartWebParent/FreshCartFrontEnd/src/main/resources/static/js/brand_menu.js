document.addEventListener('DOMContentLoaded', function () {

    const currentBrand = document.getElementById('currentBrand')?.value;

    document.querySelectorAll('.brand-filter').forEach(checkbox => {
        // Auto tick brand hiện tại
        if (checkbox.value === currentBrand) {
            checkbox.checked = true;
            checkbox.disabled = true;
        }

        // Khi tick brand bất kỳ → chuyển trang
        checkbox.addEventListener('change', function() {
            if (this.checked) {
                handleFilterChange('brand', this.value, true);
            }
        });
    });


});
