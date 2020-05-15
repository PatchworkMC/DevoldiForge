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
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Rotation3;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraftforge.client.model.pipeline.BakedQuadBuilder;

public abstract class SimpleModelFontRenderer extends TextRenderer {

    private float r, g, b, a;
    private final Rotation3 transform;
    private ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
    private final Vector3f normal = new Vector3f(0, 0, 1);
    private final Direction orientation;
    private boolean fillBlanks = false;

    private Sprite sprite;

    public SimpleModelFontRenderer(GameOptions settings, Identifier font, TextureManager manager, boolean isUnicode, Matrix4f matrix)
    {
        super(manager, null);
        this.transform = new Rotation3(matrix);
        transform.transformNormal(normal);
        orientation = Direction.getFacing(normal.getX(), normal.getY(), normal.getZ());
    }

    public void setSprite(Sprite sprite)
    {
        this.sprite = sprite;
    }

    public void setFillBlanks(boolean fillBlanks)
    {
        this.fillBlanks = fillBlanks;
    }

    private final Vector4f vec = new Vector4f();

    private void addVertex(BakedQuadBuilder quadBuilder, float x, float y, float u, float v)
    {
        ImmutableList<VertexFormatElement> elements = quadBuilder.getVertexFormat().getElements();
        for(int e = 0; e < elements.size(); e++)
        {
            VertexFormatElement element = elements.get(e);
            switch(element.getType())
            {
                case POSITION:
                    vec.set(x, y, 0f, 1f);
                    transform.transformPosition(vec);
                    quadBuilder.put(e, vec.getX(), vec.getY(), vec.getZ(), vec.getW());
                    break;
                case COLOR:
                    quadBuilder.put(e, r, g, b, a);
                    break;
                case NORMAL:
                    //quadBuilder.put(e, normal.x, normal.y, normal.z, 1);
                    quadBuilder.put(e, 0, 0, 1, 1);
                    break;
                case UV:
                    if(element.getIndex() == 0)
                    {
                        quadBuilder.put(e, sprite.getFrameU(u * 16), sprite.getFrameV(v * 16), 0, 1);
                        break;
                    }
                    // else fallthrough to default
                default:
                    quadBuilder.put(e);
                    break;
            }
        }
    }

    public ImmutableList<BakedQuad> build()
    {
        ImmutableList<BakedQuad> ret = builder.build();
        builder = ImmutableList.builder();
        return ret;
    }
}
