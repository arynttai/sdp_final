package com.restaurant.builder.application.dto;

import com.restaurant.builder.product.Fulfillment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public record OrderRequest(@NotNull @Size(min = 1, max = 20) List<@NotNull @Valid ItemRequest> items,
                           @NotNull Fulfillment fulfillment,
                           @NotBlank @Size(max = 20) String discountCode,
                           @NotBlank @Size(min = 2, max = 60) String customerName,
                           @Min(1) @Max(30) Integer tableNumber,
                           @Size(max = 300) String note,
                           @NotNull UUID requestId) {
    public OrderRequest {
        if (items != null) items = Collections.unmodifiableList(new ArrayList<>(items));
        if (note == null) note = "";
    }
}
