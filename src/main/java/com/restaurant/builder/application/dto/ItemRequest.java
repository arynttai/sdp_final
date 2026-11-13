package com.restaurant.builder.application.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public record ItemRequest(@NotBlank @Size(max = 40) String menuItemId,
                          @Min(1) @Max(10) int quantity,
                          @NotNull @Size(max = 3) List<@NotBlank @Size(max = 40) String> toppings) {
    public ItemRequest {
        if (toppings != null) toppings = Collections.unmodifiableList(new ArrayList<>(toppings));
    }
}
