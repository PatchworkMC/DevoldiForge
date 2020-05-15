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

package net.minecraftforge.fml.packs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.ResourceNotFoundException;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;

public class DelegatingResourcePack extends AbstractFileResourcePack
{
    private final List<DelegatableResourcePack> delegates = new ArrayList<>();
    private final String name;
    private final PackResourceMetadata packInfo;
    
    public DelegatingResourcePack(String id, String name, PackResourceMetadata packInfo)
    {
        this(id, name, packInfo, Collections.emptyList());
    }

    public DelegatingResourcePack(String id, String name, PackResourceMetadata packInfo, List<DelegatableResourcePack> packs)
    {
        super(new File(id));
        this.name = name;
        this.packInfo = packInfo;
        packs.forEach(this::addDelegate);
    }

    public void addDelegate(DelegatableResourcePack pack)
    {
        synchronized(delegates)
        {
            this.delegates.add(pack);
        }
    }
    
    @Override
    public String getName()
    {
        return name;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T parseMetadata(ResourceMetadataReader<T> deserializer) throws IOException
    {
        if (deserializer.getKey().equals("pack"))
        {
            return (T) packInfo;
        }
        return null;
    }

    @Override
    public Collection<Identifier> findResources(ResourceType type, String pathIn, String pathIn2, int maxDepth, Predicate<String> filter)
    {
        synchronized(delegates)
        {
            return delegates.stream()
                    .flatMap(r -> r.findResources(type, pathIn, pathIn2, maxDepth, filter).stream())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Set<String> getNamespaces(ResourceType type)
    {
        synchronized (delegates)
        {
            return delegates.stream()
                    .flatMap(r -> r.getNamespaces(type).stream())
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public void close() throws IOException
    {
        synchronized(delegates)
        {
            for (AbstractFileResourcePack pack : delegates)
            {
                pack.close();
            }
        }
    }

    @Override
    protected InputStream openFile(String resourcePath) throws IOException
    {
        if (!resourcePath.equals("pack.png")) // Mods shouldn't be able to mess with the pack icon
        {
            synchronized (delegates)
            {
                for (DelegatableResourcePack pack : delegates)
                {
                    if (pack.containsFile(resourcePath))
                    {
                        return pack.openFile(resourcePath);
                    }
                }
            }
        }
        throw new ResourceNotFoundException(this.base, resourcePath);
    }

    @Override
    protected boolean containsFile(String resourcePath)
    {
        synchronized (delegates)
        {
            for (DelegatableResourcePack pack : delegates)
            {
                if (pack.containsFile(resourcePath))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
