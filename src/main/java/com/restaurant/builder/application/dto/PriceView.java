package com.restaurant.builder.application.dto;

import com.restaurant.strategy.context.Pricing;
import java.math.BigDecimal;

public record PriceView(BigDecimal subtotal, BigDecimal discount, BigDecimal total, String currency) {
    public static PriceView of(Pricing pricing) {
        return new PriceView(pricing.subtotal(), pricing.discount(), pricing.total(), "KZT");
    }
}
