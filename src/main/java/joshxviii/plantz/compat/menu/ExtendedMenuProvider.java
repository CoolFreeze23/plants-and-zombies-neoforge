package joshxviii.plantz.compat.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Backport of Fabric's {@code ExtendedMenuProvider}. Plain {@code player.openMenu(provider)} works:
 * NeoForge calls {@link #writeClientSideData} with the new menu, and the data is encoded with the
 * menu's {@link ExtendedMenuType} codec. NeoForge does not hand over the player there, so
 * {@link #getScreenOpeningData} receives null.
 */
public interface ExtendedMenuProvider<D> extends MenuProvider {
    D getScreenOpeningData(@Nullable ServerPlayer player);

    @Override
    @SuppressWarnings("unchecked")
    default void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        if (menu.getType() instanceof ExtendedMenuType<?, ?> type) {
            ((ExtendedMenuType<?, D>) type).streamCodec().encode(buffer, getScreenOpeningData(null));
        }
    }
}
