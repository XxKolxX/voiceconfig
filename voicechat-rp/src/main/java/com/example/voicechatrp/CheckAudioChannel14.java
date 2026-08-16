package com.example.voicechatrp;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class CheckAudioChannel14 {
    public static void main(String[] args) {
        for(Method m : VoicechatServerApi.class.getMethods()) {
            if (m.getName().equals("getPlayersInRange")) {
                System.out.println(m.getName() + " params:");
                for(Parameter p : m.getParameters()) {
                    System.out.println("  " + p.getType().getName());
                }
            }
        }
    }
}
