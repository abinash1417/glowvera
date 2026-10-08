package com.glowvera.domain;

public enum StockEffect {
    NONE,
    COMMIT,   // reserved -> deducted (pending order got paid/confirmed)
    RELEASE,  // reserved -> available again (pending order cancelled/failed/expired)
    RESTOCK   // deducted -> back on the shelf (paid order cancelled)
}
