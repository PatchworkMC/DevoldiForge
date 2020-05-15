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

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.util.math.Rotation3;

/**
 * Simple implementation of IModelState via a map and a default value.
 */
public final class SimpleModelTransform implements ModelBakeSettings
{
    public static final SimpleModelTransform IDENTITY = new SimpleModelTransform(Rotation3.identity());

    private final ImmutableMap<?, Rotation3> map;
    private final Rotation3 base;

    public SimpleModelTransform(ImmutableMap<?, Rotation3> map)
    {
        this(map, Rotation3.identity());
    }

    public SimpleModelTransform(Rotation3 base)
    {
        this(ImmutableMap.of(), base);
    }

    public SimpleModelTransform(ImmutableMap<?, Rotation3> map, Rotation3 base)
    {
        this.map = map;
        this.base = base;
    }

    @Override
    public Rotation3 getRotation()
    {
        return base;
    }

    @Override
    public Rotation3 getPartTransformation(Object part)
    {
        return map.getOrDefault(part, Rotation3.identity());
    }
}
