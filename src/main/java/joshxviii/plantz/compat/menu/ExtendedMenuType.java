package joshxviii.plantz.compat.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;

/**
 * Backport of Fabric's {@code ExtendedMenuType}: a menu type whose client-side constructor receives typed
 * opening data, decoded with {@link #streamCodec()}. Built on NeoForge's {@link IContainerFactory}.
 */
public class ExtendedMenuType<T extends AbstractContainerMenu, D> extends MenuType<T> {
    @FunctionalInterface
    public interface Factory<T extends AbstractContainerMenu, D> {
        T create(int containerId, Inventory inventory, D data);
    }

    private final StreamCodec<? super RegistryFriendlyByteBuf, D> streamCodec;

    public ExtendedMenuType(Factory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> streamCodec) {
        super(containerFactory(factory, streamCodec), FeatureFlags.DEFAULT_FLAGS);
        this.streamCodec = streamCodec;
    }

    private static <T extends AbstractContainerMenu, D> IContainerFactory<T> containerFactory(Factory<T, D> factory, StreamCodec<? super RegistryFriendlyByteBuf, D> codec) {
        return (containerId, inventory, buf) -> factory.create(containerId, inventory, codec.decode(buf));
    }

    public StreamCodec<? super RegistryFriendlyByteBuf, D> streamCodec() {
        return streamCodec;
    }
}
