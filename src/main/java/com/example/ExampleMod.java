package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ExampleMod implements ClientModInitializer {
    private boolean isMacroActive = false;
    private boolean isFighting = false;
    private Entity currentTarget = null;
    private static KeyBinding toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autofish.toggle", 
                InputUtil.Type.KEYSYM, 
                GLFW.GLFW_KEY_V, 
                "category.autofish"
        ));

        ClientReceiveMessageEvents.OVERLAY.register((message, indicator) -> {
            if (!isMacroActive) return;
            String text = message.getString();
            if (text.contains("❤")) checkFailsafe(text);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                isMacroActive = !isMacroActive;
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Макрос: " + (isMacroActive ? "ВКЛ" : "ВЫКЛ")), false);
                }
            }
            if (!isMacroActive || client.player == null) return;

            Entity threat = getNearestSeaCreature(client, 4.0f);
            if (threat != null && threat.isAlive()) {
                isFighting = true;
                currentTarget = threat;
            } else {
                isFighting = false;
                currentTarget = null;
            }

            if (isFighting) handleFighting(client);
            else handleFishing(client);
        });
    }

    private void checkFailsafe(String text) {
        int currentHp = 1000, maxHp = 1000;     
        if (maxHp > 0 && ((float) currentHp / maxHp) < 0.20f) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) client.player.networkHandler.sendChatCommand("hub"); 
            isMacroActive = false;
        }
    }

    private void handleFishing(MinecraftClient client) {
        if (client.player.getInventory().selectedSlot != 0) client.player.getInventory().selectedSlot = 0;
        if (client.player.fishHook == null) rightClickMouse(client); 
        else if (detectBite(client.player.fishHook)) rightClickMouse(client); 
    }

    private void handleFighting(MinecraftClient client) {
        if (client.player.getInventory().selectedSlot != 1) client.player.getInventory().selectedSlot = 1;
        smoothLookAt(currentTarget);
    }

    private Entity getNearestSeaCreature(MinecraftClient client, float radius) { return null; }
    private boolean detectBite(Entity bobber) { return false; }
    private void smoothLookAt(Entity target) {}
    private void rightClickMouse(MinecraftClient client) {}
}
