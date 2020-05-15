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

package net.minecraftforge.event.terraingen;

import net.minecraft.world.level.LevelGeneratorType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.Event.HasResult;

/**
 * WorldTypeEvent is fired when an event involving the world occurs.<br>
 * If a method utilizes this {@link Event} as its parameter, the method will
 * receive every child event of this class.<br>
 * <br>
 * {@link #worldType} contains the WorldType of the world this event is occurring in.<br>
 * <br>
 * All children of this event are fired on the {@link MinecraftForge#TERRAIN_GEN_BUS}.<br>
 **/
public class WorldTypeEvent extends Event
{
    private final LevelGeneratorType worldType;

    public WorldTypeEvent(LevelGeneratorType worldType)
    {
        this.worldType = worldType;
    }

    public LevelGeneratorType getWorldType()
    {
        return worldType;
    }

    /**
     * BiomeSize is fired when vanilla Minecraft attempts to generate biomes.<br>
     * This event is fired during biome generation in
     * {@link GenLayer#initializeAllBiomeGenerators(long, WorldType, ChunkProviderSettings)}. <br>
     * <br>
     * {@link #originalSize} the original size of the Biome. <br>
     * {@link #newSize} the new size of the biome. Initially set to the {@link #originalSize}. <br>
     * If {@link #newSize} is set to a new value, that value will be used for the Biome size. <br>
     * <br>
     * This event is not {@link Cancelable}.<br>
     * <br>
     * This event does not have a result. {@link HasResult} <br>
     * <br>
     * This event is fired on the {@link MinecraftForge#TERRAIN_GEN_BUS}.<br>
     **/
    public static class BiomeSize extends WorldTypeEvent
    {
        private final int originalSize;
        private int newSize;

        public BiomeSize(LevelGeneratorType worldType, int original)
        {
            super(worldType);
            originalSize = original;
            setNewSize(original);
        }

        public int getOriginalSize()
        {
            return originalSize;
        }

        public int getNewSize()
        {
            return newSize;
        }

        public void setNewSize(int newSize)
        {
            this.newSize = newSize;
        }
    }

}
