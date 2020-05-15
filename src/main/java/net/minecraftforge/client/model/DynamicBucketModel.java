/*
 * Minecraft Forge
 * Copyright (c) 2016-2019.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation.Mode;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.Rotation3;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.VanillaResourceType;
import net.minecraftforge.versions.forge.ForgeVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class DynamicBucketModel implements IModelGeometry<DynamicBucketModel>
{
    private static final Logger LOGGER = LogManager.getLogger();
    public static final ModelIdentifier LOCATION = new ModelIdentifier(new Identifier(ForgeVersion.MOD_ID, "dynbucket"), "inventory");

    // minimal Z offset to prevent depth-fighting
    private static final float NORTH_Z_COVER = 7.496f / 16f;
    private static final float SOUTH_Z_COVER = 8.504f / 16f;
    private static final float NORTH_Z_FLUID = 7.498f / 16f;
    private static final float SOUTH_Z_FLUID = 8.502f / 16f;

    @Nonnull
    private final Fluid fluid;

    private final boolean flipGas;
    private final boolean tint;
    private final boolean coverIsMask;

    public DynamicBucketModel(Fluid fluid, boolean flipGas, boolean tint, boolean coverIsMask)
    {
        this.fluid = fluid;
        this.flipGas = flipGas;
        this.tint = tint;
        this.coverIsMask = coverIsMask;
    }

    /**
     * Returns a new ModelDynBucket representing the given fluid, but with the same
     * other properties (flipGas, tint, coverIsMask).
     */
    public DynamicBucketModel withFluid(Fluid newFluid)
    {
        return new DynamicBucketModel(newFluid, flipGas, tint, coverIsMask);
    }

    @Override
    public net.minecraft.client.render.model.BakedModel bake(IModelConfiguration owner, ModelLoader bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelTransform, ModelItemPropertyOverrideList overrides, Identifier modelLocation)
    {
        SpriteIdentifier particleLocation = owner.resolveTexture("particle");
        if (MissingSprite.getMissingSpriteId().equals(particleLocation.getTextureId()))
        {
            particleLocation = null;
        }

        SpriteIdentifier baseLocation = owner.resolveTexture("base");
        if (MissingSprite.getMissingSpriteId().equals(baseLocation.getTextureId()))
        {
            baseLocation = null;
        }

        SpriteIdentifier fluidMaskLocation = owner.resolveTexture("fluid");
        if (MissingSprite.getMissingSpriteId().equals(fluidMaskLocation.getTextureId()))
        {
            fluidMaskLocation = null;
        }

        SpriteIdentifier coverLocation = owner.resolveTexture("cover");
        if (!MissingSprite.getMissingSpriteId().equals(coverLocation.getTextureId()))
        {
            // cover (the actual item around the other two)
            coverLocation = null;
        }

        ModelBakeSettings transformsFromModel = owner.getCombinedTransform();

        ImmutableMap<Mode, Rotation3> transformMap = transformsFromModel != null ?
                        PerspectiveMapWrapper.getTransforms(new ModelTransformComposition(transformsFromModel, modelTransform)) :
                        PerspectiveMapWrapper.getTransforms(modelTransform);

        Sprite particleSprite = particleLocation != null ? spriteGetter.apply(particleLocation) : null;

        // if the fluid is lighter than air, will manipulate the initial state to be rotated 180deg to turn it upside down
        if (flipGas && fluid != Fluids.EMPTY && fluid.getAttributes().isLighterThanAir())
        {
            modelTransform = new ModelTransformComposition(modelTransform, new SimpleModelTransform(new Rotation3(null, new Quaternion(0, 0, 1, 0), null, null)));
        }

        Rotation3 transform = modelTransform.getRotation();

        Sprite fluidSprite = fluid != Fluids.EMPTY ? spriteGetter.apply(ForgeHooksClient.getBlockMaterial(fluid.getAttributes().getStillTexture())) : null;

        if (particleSprite == null) particleSprite = fluidSprite;

        ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();

        if (baseLocation != null)
        {
            // build base (insidest)
            builder.addAll(ItemLayerModel.getQuadsForSprites(ImmutableList.of(baseLocation), transform, spriteGetter));
        }

        if (fluidMaskLocation != null && fluidSprite != null)
        {
            Sprite templateSprite = spriteGetter.apply(fluidMaskLocation);
            if (templateSprite != null)
            {
                // build liquid layer (inside)
                builder.addAll(ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, NORTH_Z_FLUID, Direction.NORTH, tint ? fluid.getAttributes().getColor() : 0xFFFFFFFF, 1));
                builder.addAll(ItemTextureQuadConverter.convertTexture(transform, templateSprite, fluidSprite, SOUTH_Z_FLUID, Direction.SOUTH, tint ? fluid.getAttributes().getColor() : 0xFFFFFFFF, 1));
            }
        }

        if (coverLocation != null && (!coverIsMask || baseLocation != null))
        {
            // cover (the actual item around the other two)
            Sprite coverSprite = spriteGetter.apply(coverLocation);
            if (coverSprite != null)
            {
                if (coverIsMask)
                {
                    Sprite baseSprite = spriteGetter.apply(baseLocation);
                    builder.addAll(ItemTextureQuadConverter.convertTexture(transform, coverSprite, baseSprite, NORTH_Z_COVER, Direction.NORTH, 0xFFFFFFFF, 1));
                    builder.addAll(ItemTextureQuadConverter.convertTexture(transform, coverSprite, baseSprite, SOUTH_Z_COVER, Direction.SOUTH, 0xFFFFFFFF, 1));
                }
                else
                {
                    builder.add(ItemTextureQuadConverter.genQuad(transform, 0, 0, 16, 16, NORTH_Z_COVER, coverSprite, Direction.NORTH, 0xFFFFFFFF, 2));
                    builder.add(ItemTextureQuadConverter.genQuad(transform, 0, 0, 16, 16, SOUTH_Z_COVER, coverSprite, Direction.SOUTH, 0xFFFFFFFF, 2));
                    if (particleSprite == null)
                    {
                        particleSprite = coverSprite;
                    }
                }
            }
        }

        return new BakedModel(bakery, owner, this, builder.build(), particleSprite, Maps.immutableEnumMap(transformMap), Maps.newHashMap(), transform.isIdentity(), modelTransform, owner.isSideLit());
    }

    @Override
    public Collection<SpriteIdentifier> getTextures(IModelConfiguration owner, Function<Identifier, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
    {
        Set<SpriteIdentifier> texs = Sets.newHashSet();

        texs.add(owner.resolveTexture("particle"));
        texs.add(owner.resolveTexture("base"));
        texs.add(owner.resolveTexture("fluid"));
        texs.add(owner.resolveTexture("cover"));

        return texs;
    }

    public enum Loader implements IModelLoader<DynamicBucketModel>
    {
        INSTANCE;

        @Override
        public IResourceType getResourceType()
        {
            return VanillaResourceType.MODELS;
        }

        @Override
        public void apply(ResourceManager resourceManager)
        {
            // no need to clear cache since we create a new model instance
        }

        @Override
        public void onResourceManagerReload(ResourceManager resourceManager, Predicate<IResourceType> resourcePredicate)
        {
            // no need to clear cache since we create a new model instance
        }

        @Override
        public DynamicBucketModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents)
        {
            if (!modelContents.has("fluid"))
                throw new RuntimeException("Bucket model requires 'fluid' value.");

            Identifier fluidName = new Identifier(modelContents.get("fluid").getAsString());

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidName);

            boolean flip = false;
            if (modelContents.has("flipGas"))
            {
                flip = modelContents.get("flipGas").getAsBoolean();
            }

            boolean tint = true;
            if (modelContents.has("applyTint"))
            {
                tint = modelContents.get("applyTint").getAsBoolean();
            }

            boolean coverIsMask = true;
            if (modelContents.has("coverIsMask"))
            {
                coverIsMask = modelContents.get("coverIsMask").getAsBoolean();
            }

            // create new model with correct liquid
            return new DynamicBucketModel(fluid, flip, tint, coverIsMask);
        }
    }

    private static final class ContainedFluidOverrideHandler extends ModelItemPropertyOverrideList
    {
        private final ModelLoader bakery;
        
        private ContainedFluidOverrideHandler(ModelLoader bakery)
        {
            this.bakery = bakery;
        }

        @Override
        public net.minecraft.client.render.model.BakedModel apply(net.minecraft.client.render.model.BakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable LivingEntity entity)
        {
            return FluidUtil.getFluidContained(stack)
                    .map(fluidStack -> {
                        BakedModel model = (BakedModel)originalModel;

                        Fluid fluid = fluidStack.getFluid();
                        String name = fluid.getRegistryName().toString();

                        if (!model.cache.containsKey(name))
                        {
                            DynamicBucketModel parent = model.parent.withFluid(fluid);
                            net.minecraft.client.render.model.BakedModel bakedModel = parent.bake(model.owner, bakery, net.minecraftforge.client.model.ModelLoader.defaultTextureGetter(), model.originalTransform, model.getItemPropertyOverrides(), new Identifier("forge:bucket_override"));
                            model.cache.put(name, bakedModel);
                            return bakedModel;
                        }

                        return model.cache.get(name);
                    })
                    // not a fluid item apparently
                    .orElse(originalModel); // empty bucket
        }
    }

    // the dynamic bucket is based on the empty bucket
    private static final class BakedModel extends BakedItemModel
    {
        private final IModelConfiguration owner;
        private final DynamicBucketModel parent;
        private final Map<String, net.minecraft.client.render.model.BakedModel> cache; // contains all the baked models since they'll never change
        private final ModelBakeSettings originalTransform;
        private final boolean isSideLit;

        BakedModel(ModelLoader bakery,
                   IModelConfiguration owner, DynamicBucketModel parent,
                   ImmutableList<BakedQuad> quads,
                   Sprite particle,
                   ImmutableMap<Mode, Rotation3> transforms,
                   Map<String, net.minecraft.client.render.model.BakedModel> cache,
                   boolean untransformed,
                   ModelBakeSettings originalTransform, boolean isSideLit)
        {
            super(quads, particle, transforms, new ContainedFluidOverrideHandler(bakery), untransformed, isSideLit);
            this.owner = owner;
            this.parent = parent;
            this.cache = cache;
            this.originalTransform = originalTransform;
            this.isSideLit = isSideLit;
        }
    }

}
