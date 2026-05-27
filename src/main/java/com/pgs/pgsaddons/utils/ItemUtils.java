package com.pgs.pgsaddons.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ItemUtils {
    /**
     * Gets the texture from a player head item
     */
    @NotNull
    public static String getHeadTexture(@NotNull ItemStack stack) {
        if (stack.isOf(Items.PLAYER_HEAD) && stack.contains(DataComponentTypes.PROFILE)) {
            ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
            if (profile == null) return "";

            GameProfile gameProfile = profile.getGameProfile();
            if (gameProfile == null) return "";

            String base64 = gameProfile.properties().get("textures").stream()
                    .filter(Objects::nonNull)
                    .map(Property::value)
                    .findFirst()
                    .orElse("");

            if (base64.isEmpty()) return "";

            try {
                String decoded = new String(java.util.Base64.getDecoder().decode(base64), java.nio.charset.StandardCharsets.UTF_8);
                // Extract the texture ID from the URL: http://textures.minecraft.net/texture/<ID>
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("texture/([a-zA-Z0-9]+)").matcher(decoded);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception ignored) {}
        }
        return "";
    }
}
