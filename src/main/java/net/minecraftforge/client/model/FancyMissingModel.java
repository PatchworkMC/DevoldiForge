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

import java.util.function.Function;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rotation3;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.model.TransformationHelper;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class FancyMissingModel implements UnbakedModel
{
    private static final Identifier font = new Identifier("minecraft", "textures/font/ascii.png");
    private static final SpriteIdentifier font2 = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("minecraft", "font/ascii"));
    private static final Rotation3 smallTransformation = new Rotation3(null, null, new Vector3f(.25f, .25f, .25f), null)
            .blockCenterToCorner();
    private static final SimpleModelFontRenderer fontRenderer = Util.make(() -> {
        float [] mv = new float[16];
        mv[2*4+0] = 1f / 128f;
        mv[0*4+1] =mv[1*4+2] = -mv[2*4+0];
        mv[3*4+3] = 1;
        mv[0*4+3] = 1;
        mv[0*4+3] = 1 + 1f / 0x100;
        mv[0*4+3] = 0;
        Matrix4f m = new Matrix4f(mv);
        return new SimpleModelFontRenderer(
            MinecraftClient.getInstance().options,
            font,
            MinecraftClient.getInstance().getTextureManager(),
            false,
            m
        ) {/* TODO Implement once SimpleModelFontRenderer is fixed
            @Override
            protected float renderUnicodeChar(char c, boolean italic)
            {
                return super.renderDefaultChar(126, italic);
            }
      */};
    });

    private final UnbakedModel missingModel;
    private final String message;

    public FancyMissingModel(UnbakedModel missingModel, String message)
    {
        this.missingModel = missingModel;
        this.message = message;
    }

    @Override
    public Collection<SpriteIdentifier> getTextureDependencies(Function<Identifier, UnbakedModel> modelGetter, Set<com.mojang.datafixers.util.Pair<String, String>> missingTextureErrors)
    {
        return ImmutableList.of(font2);
    }

    @Override
    public Collection<Identifier> getModelDependencies()
    {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public BakedModel bake(ModelLoader bakery, Function<SpriteIdentifier, Sprite> spriteGetter, ModelBakeSettings modelTransform, Identifier modelLocation)
    {
        BakedModel bigMissing = missingModel.bake(bakery, spriteGetter, modelTransform, modelLocation);
        ModelTransformComposition smallState = new ModelTransformComposition(modelTransform, new SimpleModelTransform(smallTransformation));
        BakedModel smallMissing = missingModel.bake(bakery, spriteGetter, smallState, modelLocation);
        return new net.minecraftforge.client.model.FancyMissingModel.BakedModel(bigMissing, smallMissing, fontRenderer, message, spriteGetter.apply(font2));
    }

    static final class BakedModel implements BakedModel
    {
        private final SimpleModelFontRenderer fontRenderer;
        private final String message;
        private final Sprite fontTexture;
        private final BakedModel missingModel;
        private final BakedModel otherModel;
        private final boolean big;
        private ImmutableList<BakedQuad> quads;

        public BakedModel(BakedModel bigMissing, BakedModel smallMissing, SimpleModelFontRenderer fontRenderer, String message, Sprite fontTexture)
        {
            this.missingModel = bigMissing;
            otherModel = new net.minecraftforge.client.model.FancyMissingModel.BakedModel(smallMissing, fontRenderer, message, fontTexture, this);
            this.big = true;
            this.fontRenderer = fontRenderer;
            this.message = message;
            this.fontTexture = fontTexture;
        }

        public BakedModel(BakedModel smallMissing, SimpleModelFontRenderer fontRenderer, String message, Sprite fontTexture, net.minecraftforge.client.model.FancyMissingModel.BakedModel big)
        {
            this.missingModel = smallMissing;
            otherModel = big;
            this.big = false;
            this.fontRenderer = fontRenderer;
            this.message = message;
            this.fontTexture = fontTexture;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand)
        {
            if (side == null)
            {
                if (quads == null)
                {
                    fontRenderer.setSprite(fontTexture);
                    fontRenderer.setFillBlanks(true);
                    String[] lines = message.split("\\r?\\n");
                    List<String> splitLines = Lists.newArrayList();
                    for (int y = 0; y < lines.length; y++)
                    {
                        splitLines.addAll(fontRenderer.wrapStringToWidthAsList(lines[y], 0x80));
                    }
                    for (int y = 0; y < splitLines.size(); y++)
                    {
                        fontRenderer.draw(splitLines.get(y), 0, ((y - splitLines.size() / 2f) * fontRenderer.fontHeight) + 0x40, 0xFF00FFFF);
                    }
                    ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
                    builder.addAll(missingModel.getQuads (state, side, rand));
                    builder.addAll(fontRenderer.build());
                    quads = builder.build();
                }
                return quads;
            }
            return missingModel.getQuads (state, side, rand);
        }

        @Override
        public boolean useAmbientOcclusion() { return true; }

        @Override
        public boolean hasDepth() { return false; }

        @Override
        public boolean isSideLit() { return false; }

        @Override
        public boolean isBuiltin() { return false; }

        @Override
        public Sprite getSprite() { return fontTexture; }

        @Override
        public ModelItemPropertyOverrideList getItemPropertyOverrides() { return ModelItemPropertyOverrideList.EMPTY; }

        @Override
        public boolean doesHandlePerspectives()
        {
            return true;
        }

        @Override
        public BakedModel handlePerspective(ModelTransformation.Mode cameraTransformType, MatrixStack mat)
        {
            Rotation3 transform = Rotation3.identity();
            boolean big = true;
            switch (cameraTransformType)
            {

                case THIRD_PERSON_LEFT_HAND:
                    break;
                case THIRD_PERSON_RIGHT_HAND:
                    break;
                case FIRST_PERSON_LEFT_HAND:
                    transform = new Rotation3(new Vector3f(-0.62f, 0.5f, -.5f), new Quaternion(1, -1, -1, 1), null, null);
                    big = false;
                    break;
                case FIRST_PERSON_RIGHT_HAND:
                    transform = new Rotation3(new Vector3f(-0.5f, 0.5f, -.5f), new Quaternion(1, 1, 1, 1), null, null);
                    big = false;
                    break;
                case HEAD:
                    break;
                case GUI:
                    if (ForgeConfig.CLIENT.zoomInMissingModelTextInGui.get())
                    {
                        transform = new Rotation3(null, new Quaternion(1, 1, 1, 1), new Vector3f(4, 4, 4), null);
                        big = false;
                    }
                    else
                    {
                        transform = new Rotation3(null, new Quaternion(1, 1, 1, 1), null, null);
                        big = true;
                    }
                    break;
                case FIXED:
                    transform = new Rotation3(null, new Quaternion(-1, -1, 1, 1), null, null);
                    break;
                default:
                    break;
            }
            mat.peek().getModel().multiply(transform.getMatrix());
            if (big != this.big)
            {
                return otherModel;
            }
            return this;
        }
    }
}
