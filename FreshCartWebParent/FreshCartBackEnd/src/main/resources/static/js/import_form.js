$(document).ready(function () {
    let rowIndex = 0; // Initialize row index

    // Open modal on button click
    $("#addProductBtn").on("click", function () {
        $("#addProductModal").modal('show');
    });

    // Input validation for product amount and cost
    $("#productAmount").on("input", function () {
        let value = $(this).val();
        if (value < 0) {
            $(this).val(1);
            showWarningMessage("Amount cannot be negative.");
        }
    });
    $("#productCost").on("input", function () {
        let value = $(this).val();
        if (value < 0) {
            $(this).val(1);
            showWarningMessage("Cost cannot be negative.");
        }
    });

    // Save new product logic
    $("#saveProductBtn").on("click", function () {
        const selectedProductId = $("#productSearch").val();
        const selectedOption = $(`#productList option[value="${selectedProductId}"]`);

        // Check if the selected product exists in the datalist
        if (selectedOption.length === 0) {
            showWarningMessage("Please select a valid product from the list");
            return;
        }

        // Check the product in the added list but want to add again
        if ($(`#productTableBody tr td:first-child:contains(${selectedProductId})`).length > 0) {
            showWarningMessage("This product is already added to the list. Please delete it to edit or add.");
            return;
        }

        const selectedProductName = selectedOption.text();
        const productImage = selectedOption.data("image-url");
        const productAmount = $("#productAmount").val();
        const productCost = $("#productCost").val();
        const productUnit = $("#productUnit").val();

        if(selectedProductId.length === 0){
            showWarningMessage("Please fill Product ID");
        }
        else if(productAmount.length === 0){
            showWarningMessage("Please fill Product Amount");
        }
        else if(productCost.length === 0){
            showWarningMessage("Please fill Product Cost");
        }

        if (selectedProductId && productAmount && productCost) {
            // Calculate total cost
            const totalCost = parseFloat(productAmount) * parseFloat(productCost);

            // Format số tiền với 2 số thập phân và dấu phẩy phân cách hàng nghìn
            function formatCurrency(number) {
                return new Intl.NumberFormat('en-US', {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2
                }).format(number);
            }

            // Create a new row for the selected product
            const newRow = `
                <tr data-row-index="${rowIndex}">
                    <td>${selectedProductId}</td>
                    <td>
                        <div class="d-flex align-items-center">
                            <img src="${productImage}" alt="${selectedProductName}" 
                                 style="width: 60px; height: 60px; object-fit: cover;" 
                                 class="rounded me-3"/>
                            <div>
                                <h6 class="mb-0">${selectedProductName}</h6>
                            </div>
                        </div>
                    </td>
                    <td>${productAmount}</td>
                    <td>$${formatCurrency(productCost)}</td>
                    <td>$${formatCurrency(totalCost)}</td>
                    <td>${productUnit}</td>
                    <td>
                        <a href="javascript:void(0);"
                           class="fas fa-trash fa-2x icon-dark link-delete" 
                           data-row-index="${rowIndex}" 
                           data-product-id="${selectedProductId}" 
                           data-product-name="${selectedProductName}"></a>
                    </td>
                </tr>
            `;

            $("#productTableBody").append(newRow);

            // Scroll to the bottom of the table
            const container = $("#productTableBody").closest("div");
            container.animate({ scrollTop: container.prop("scrollHeight") }, 500);

            // Add hidden inputs for form submission
            const hiddenInputs = `
                <div id="hiddenInputs-${rowIndex}">
                    <input type="hidden" name="productIds" value="${selectedProductId}" />
                    <input type="hidden" name="productAmounts" value="${productAmount}" />
                    <input type="hidden" name="productCosts" value="${productCost}" />
                </div>
            `;

            $("#hiddenInputsContainer").append(hiddenInputs);

            // Increment row index
            rowIndex++;

            // Close modal and reset modal form inputs
            $("#addProductModal").modal('hide');
            $("#productAmount").val(1);
            $("#productCost").val(1.0);
        }
    });

    // Delete product logic with reinsertion in correct position using map
    $("#productTableBody").on("click", ".link-delete", function () {
        const indexToDelete = $(this).data("row-index");

        // Remove the table row with the matching index
        $(`tr[data-row-index="${indexToDelete}"]`).remove();

        // Remove associated hidden inputs
        $(`#hiddenInputs-${indexToDelete}`).remove();
    });

    // Cancel button logic
    $("#buttonCancel").on("click", function () {
        window.history.back();
    });

    // Form submission check
    $("form").on("submit", function (event) {
        if ($("#productTableBody tr").length === 0) {
            showWarningMessage("Please add at least one product before saving.");
            event.preventDefault(); // Prevent form submission
        }
    });
});
