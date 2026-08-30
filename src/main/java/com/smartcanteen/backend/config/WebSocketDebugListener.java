package com.smartcanteen.backend.config;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class WebSocketDebugListener {
    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        System.out.println("🔥 STOMP CONNECT RECEIVED");
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        System.out.println("🔥 STOMP SUBSCRIBE RECEIVED");
    }

}



