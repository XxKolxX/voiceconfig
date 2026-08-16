package com.example.voicechatrp;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import java.lang.reflect.Method;

public class CheckAudioChannel13 {
    public static void main(String[] args) {
        for(Method m : VoicechatServerApi.class.getMethods()) {
            if (m.getName().toLowerCase().contains("player")) {
                System.out.println(m.getName() + " -> " + m.getReturnType().getName());
            }
        }
    }
}
