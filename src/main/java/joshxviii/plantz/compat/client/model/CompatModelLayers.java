package joshxviii.plantz.compat.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import static joshxviii.plantz.UtilsKt.pazResource;

/** 26.1 vanilla model layers that 1.21.1 does not have (registered under the mod's namespace). */
public final class CompatModelLayers {
    private static final float INNER_ARMOR_DEFORMATION = 0.5F;
    private static final float OUTER_ARMOR_DEFORMATION = 1.0F;
    private static final float[] BABY_INNER_ARMOR_DEFORMATION = {-0.1F, 0.3F, 0.3F};
    private static final float[] BABY_OUTER_ARMOR_DEFORMATION = {-0.1F, 0.5F, 0.3F};

    public static final ModelLayerLocation ZOMBIE_BABY = new ModelLayerLocation(pazResource("zombie_baby"), "main");
    public static final ArmorModelSet<ModelLayerLocation> ZOMBIE_ARMOR = armorSet("zombie");
    public static final ArmorModelSet<ModelLayerLocation> ZOMBIE_BABY_ARMOR = armorSet("zombie_baby");

    private CompatModelLayers() {
    }

    private static ArmorModelSet<ModelLayerLocation> armorSet(String model) {
        return new ArmorModelSet<>(
            new ModelLayerLocation(pazResource(model), "helmet"),
            new ModelLayerLocation(pazResource(model), "chestplate"),
            new ModelLayerLocation(pazResource(model), "leggings"),
            new ModelLayerLocation(pazResource(model), "boots")
        );
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ZOMBIE_BABY, () -> BabyZombieModel.createBodyLayer(CubeDeformation.NONE));
        registerArmorSet(event, ZOMBIE_ARMOR, HumanoidModel.createArmorMeshSet(INNER_ARMOR_DEFORMATION, OUTER_ARMOR_DEFORMATION));
        registerArmorSet(event, ZOMBIE_BABY_ARMOR, HumanoidModel.createBabyArmorMeshSet(BABY_INNER_ARMOR_DEFORMATION, BABY_OUTER_ARMOR_DEFORMATION));
    }

    private static void registerArmorSet(EntityRenderersEvent.RegisterLayerDefinitions event, ArmorModelSet<ModelLayerLocation> locations,
                                         ArmorModelSet<MeshDefinition> meshes) {
        ArmorModelSet<LayerDefinition> layers = meshes.map(mesh -> LayerDefinition.create(mesh, 64, 32));
        event.registerLayerDefinition(locations.head(), layers::head);
        event.registerLayerDefinition(locations.chest(), layers::chest);
        event.registerLayerDefinition(locations.legs(), layers::legs);
        event.registerLayerDefinition(locations.feet(), layers::feet);
    }
}
