package com.restaurant.bridge.implementation;

public final class HtmlReceiptRenderer implements ReceiptRenderer {
    @Override
    public String render(ReceiptContent content) {
        StringBuilder html = new StringBuilder("<section class=\"receipt\">\n");
        appendElement(html, "h1", content.title());
        for (String detail : content.details()) {
            appendElement(html, "p", detail);
        }
        html.append("<ul>\n");
        for (String item : content.items()) {
            appendElement(html, "li", item);
        }
        html.append("</ul>\n");
        for (String total : content.totals()) {
            appendElement(html, "p", total);
        }
        return html.append("</section>\n").toString();
    }

    private void appendElement(StringBuilder html, String tag, String value) {
        html.append('<').append(tag).append('>').append(escape(value))
            .append("</").append(tag).append(">\n");
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
