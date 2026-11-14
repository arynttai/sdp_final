package com.restaurant.builder.application.dto;

import com.restaurant.builder.product.Fulfillment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public record QuoteRequest(@NotNull @Size(min = 1, max = 20) List<@NotNull @Valid ItemRequest> items,
                           @NotNull Fulfillment fulfillment,
                           @NotBlank @Size(max = 20) String discountCode) {
    public QuoteRequest {
        if (items != null) items = Collections.unmodifiableList(new ArrayList<>(items));
    }
}
