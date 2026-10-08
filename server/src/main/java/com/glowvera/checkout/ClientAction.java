package com.glowvera.checkout;

import java.util.Map;

public sealed interface ClientAction permits ClientAction.PayHereForm, ClientAction.WhatsAppLink {

    record PayHereForm(String type, String actionUrl, String method, Map<String, String> fields)
            implements ClientAction {

        public static PayHereForm of(String actionUrl, Map<String, String> fields) {
            return new PayHereForm("PAYHERE_FORM", actionUrl, "POST", fields);
        }
    }

    // The browser opens a wa.me link; the message is also returned for a preview or copy fallback.//
    record WhatsAppLink(String type, String url, String message) implements ClientAction {

        public static WhatsAppLink of(String url, String message) {
            return new WhatsAppLink("WHATSAPP_LINK", url, message);
        }
    }
}
