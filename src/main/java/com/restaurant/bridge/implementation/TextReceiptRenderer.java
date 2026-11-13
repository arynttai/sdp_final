package com.restaurant.bridge.implementation;

public final class TextReceiptRenderer implements ReceiptRenderer {
    @Override
    public String render(ReceiptContent content) {
        StringBuilder text = new StringBuilder(content.title()).append('\n');
        for (String detail : content.details()) {
            text.append(detail).append('\n');
        }
        for (String item : content.items()) {
            text.append("  ").append(item).append('\n');
        }
        for (String total : content.totals()) {
            text.append(total).append('\n');
        }
        return text.toString();
    }
}
