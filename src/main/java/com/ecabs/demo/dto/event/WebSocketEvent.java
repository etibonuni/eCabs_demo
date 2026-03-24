package com.ecabs.demo.dto.event;

public class WebSocketEvent<T> {
    private EventType type;
    private T payload;

    public WebSocketEvent() {}

    public WebSocketEvent(EventType type, T payload) {
        this.type = type;
        this.payload = payload;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
