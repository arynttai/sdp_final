package com.restaurant.observer;

import com.restaurant.observer.concrete.CustomerObserver;
import com.restaurant.observer.concrete.KitchenObserver;
import com.restaurant.observer.event.OrderNotification;
import com.restaurant.observer.event.OrderStatus;
import com.restaurant.observer.subject.OrderProgress;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OrderProgressTest {
    @Test void bothSubscribersReceiveEveryPublishedStatusWithTheSameEventIdentity() {
        UUID id = UUID.randomUUID();
        OrderProgress subject = new OrderProgress(id);
        var kitchen = new KitchenObserver();
        subject.subscribe(new CustomerObserver());
        subject.subscribe(kitchen);
        subject.subscribe(kitchen);
        subject.announce();
        for (int step = 0; step < 3; step++) subject.advance();

        assertThat(subject.notifications()).hasSize(8);
        for (OrderStatus status : OrderStatus.values()) {
            List<OrderNotification> events = subject.notifications().stream()
                .filter(event -> event.status() == status).toList();
            assertThat(events).hasSize(2);
            assertThat(events).extracting(OrderNotification::audience).containsExactly("CUSTOMER", "KITCHEN");
            assertThat(events).extracting(OrderNotification::orderId).containsOnly(id);
            assertThat(events).extracting(OrderNotification::occurredAt).containsOnly(events.getFirst().occurredAt());
            assertThat(events.getFirst().message()).isNotEqualTo(events.getLast().message());
        }
    }

    @Test void repeatingCurrentStatusIsIdempotentAndInvalidTransitionsPublishNothing() {
        OrderProgress subject = new OrderProgress(UUID.randomUUID());
        subject.subscribe(new CustomerObserver());
        assertThatThrownBy(subject::advance).isInstanceOf(IllegalStateException.class);
        assertThat(subject.notifications()).isEmpty();
        subject.announce();
        List<OrderNotification> placed = subject.notifications();
        subject.transitionTo(OrderStatus.PLACED);
        assertThat(subject.notifications()).isEqualTo(placed);
        assertThatThrownBy(subject::announce).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> subject.transitionTo(OrderStatus.READY)).isInstanceOf(IllegalStateException.class);
        assertThat(subject.notifications()).isEqualTo(placed);

        subject.advance();
        List<OrderNotification> preparing = subject.notifications();
        assertThatThrownBy(() -> subject.transitionTo(OrderStatus.PLACED)).isInstanceOf(IllegalStateException.class);
        assertThat(subject.notifications()).isEqualTo(preparing);
        subject.advance();
        subject.advance();
        List<OrderNotification> completed = subject.notifications();
        subject.transitionTo(OrderStatus.COMPLETED);
        assertThatThrownBy(subject::advance).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> subject.transitionTo(OrderStatus.READY)).isInstanceOf(IllegalStateException.class);
        assertThat(subject.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(subject.notifications()).isEqualTo(completed).hasSize(4);
    }

    @Test void unsubscribingOneObserverLeavesTheOtherActiveAndKeepsPastEvents() {
        OrderProgress subject = new OrderProgress(UUID.randomUUID());
        var customer = new CustomerObserver();
        var kitchen = new KitchenObserver();
        subject.subscribe(customer);
        subject.subscribe(kitchen);
        subject.announce();
        List<OrderNotification> snapshot = subject.notifications();
        subject.unsubscribe(kitchen);
        subject.advance();
        subject.advance();
        subject.advance();

        assertThat(snapshot).hasSize(2);
        assertThatThrownBy(snapshot::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThat(subject.notifications()).hasSize(5);
        assertThat(subject.notifications()).filteredOn(event -> event.audience().equals("KITCHEN")).hasSize(1);
        assertThat(subject.notifications()).filteredOn(event -> event.audience().equals("CUSTOMER"))
            .extracting(OrderNotification::status).containsExactly(OrderStatus.values());
    }

    @Test void independentSubjectsCannotShareNotifications() {
        OrderProgress first = new OrderProgress(UUID.randomUUID());
        OrderProgress second = new OrderProgress(UUID.randomUUID());
        first.subscribe(new CustomerObserver());
        second.subscribe(new CustomerObserver());
        first.announce();
        first.advance();
        assertThat(first.notifications()).hasSize(2);
        assertThat(second.notifications()).isEmpty();
    }
}
