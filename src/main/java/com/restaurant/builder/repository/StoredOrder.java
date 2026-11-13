package com.restaurant.builder.repository;

import com.restaurant.builder.product.Order;
import com.restaurant.builder.application.dto.OrderRequest;
import com.restaurant.observer.subject.OrderProgress;
import com.restaurant.strategy.context.Pricing;

public record StoredOrder(Order order, Pricing pricing, OrderProgress progress, OrderRequest request) { }
