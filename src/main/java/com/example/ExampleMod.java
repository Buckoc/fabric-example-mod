package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ExampleMod implements ClientModInitializer {

    private boolean isMacroActive = false;
    private boolean isFighting = false;
    private Entity currentTarget = null;
    private static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autofish.toggle", 
                InputConstants.Type.KEYSYM, 
                GLFW.GLFW_KEY_V, 
                "category.autofish"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Обработка нажатия клавиши
            while (toggleKey.consumeClick()) {
                isMacroActive = !isMacroActive;
                if (client.player != null) {
                    client.player.displayClientMessage(Component.literal("Макрос: " + (isMacroActive ? "ВКЛ" : "ВЫКЛ")), false);
                }
            }

            if (!isMacroActive || client.player == null) return;

            // УМНАЯ ЗАЩИТА (считывание HP напрямую из персонажа)
            float hp = client.player.getHealth();
            float maxHp = client.player.getMaxHealth();
            if (maxHp > 0 && (hp / maxHp) < 0.20f) {
                if (client.getConnection() != null) {
                    client.getConnection().sendCommand("hub");
                }
                isMacroActive = false;
                return; // Останавливаем работу в этом тике
            }

            // Проверка врагов
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

    private void handleFishing(Minecraft client) {
        if (client.player.getInventory().selected != 0) {
            client.player.getInventory().selected = 0;
        }
        if (client.player.fishing == null) rightClickMouse(client); 
        else if (detectBite(client.player.fishing)) rightClickMouse(client); 
    }

    private void handleFighting(Minecraft client) {
        if (client.player.getInventory().selected != 1) {
            client.player.getInventory().selected = 1;
        }
        smoothLookAt(currentTarget);
    }

    // --- ЗАГЛУШКИ МЕТОДОВ ---
    private Entity getNearestSeaCreature(Minecraft client, float radius) { return null; }
    private boolean detectBite(Entity bobber) { return false; }
    private void smoothLookAt(Entity target) {}
    private void rightClickMouse(Minecraft client) {}
}
