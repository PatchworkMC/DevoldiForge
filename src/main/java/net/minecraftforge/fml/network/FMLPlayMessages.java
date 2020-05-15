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

package net.minecraftforge.fml.network;

import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.Screens;
import net.minecraft.client.gui.screen.ingame.ContainerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.container.Container;
import net.minecraft.container.ContainerType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.VoronoiBiomeAccessType;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.ModDimension;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class FMLPlayMessages
{
    /**
     * Used to spawn a custom entity without the same restrictions as
     * {@link net.minecraft.network.play.server.SSpawnObjectPacket} or {@link net.minecraft.network.play.server.SSpawnMobPacket}
     *
     * To customize how your entity is created clientside (instead of using the default factory provided to the {@link EntityType})
     * see {@link EntityType.Builder#setCustomClientFactory}.
     */
    public static class SpawnEntity
    {
        private final Entity entity;
        private final int typeId;
        private final int entityId;
        private final UUID uuid;
        private final double posX, posY, posZ;
        private final byte pitch, yaw, headYaw;
        private final int velX, velY, velZ;
        private final PacketByteBuf buf;

        SpawnEntity(Entity e)
        {
            this.entity = e;
            this.typeId = Registry.ENTITY_TYPE.getRawId(e.getType());
            this.entityId = e.getEntityId();
            this.uuid = e.getUuid();
            this.posX = e.getX();
            this.posY = e.getY();
            this.posZ = e.getZ();
            this.pitch = (byte) MathHelper.floor(e.pitch * 256.0F / 360.0F);
            this.yaw = (byte) MathHelper.floor(e.yaw * 256.0F / 360.0F);
            this.headYaw = (byte) (e.getHeadYaw() * 256.0F / 360.0F);
            Vec3d vec3d = e.getVelocity();
            double d1 = MathHelper.clamp(vec3d.x, -3.9D, 3.9D);
            double d2 = MathHelper.clamp(vec3d.y, -3.9D, 3.9D);
            double d3 = MathHelper.clamp(vec3d.z, -3.9D, 3.9D);
            this.velX = (int)(d1 * 8000.0D);
            this.velY = (int)(d2 * 8000.0D);
            this.velZ = (int)(d3 * 8000.0D);
            this.buf = null;
        }

        private SpawnEntity(int typeId, int entityId, UUID uuid, double posX, double posY, double posZ,
                byte pitch, byte yaw, byte headYaw, int velX, int velY, int velZ, PacketByteBuf buf)
        {
            this.entity = null;
            this.typeId = typeId;
            this.entityId = entityId;
            this.uuid = uuid;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.pitch = pitch;
            this.yaw = yaw;
            this.headYaw = headYaw;
            this.velX = velX;
            this.velY = velY;
            this.velZ = velZ;
            this.buf = buf;
        }

        public static void encode(SpawnEntity msg, PacketByteBuf buf)
        {
            buf.writeVarInt(msg.typeId);
            buf.writeInt(msg.entityId);
            buf.writeLong(msg.uuid.getMostSignificantBits());
            buf.writeLong(msg.uuid.getLeastSignificantBits());
            buf.writeDouble(msg.posX);
            buf.writeDouble(msg.posY);
            buf.writeDouble(msg.posZ);
            buf.writeByte(msg.pitch);
            buf.writeByte(msg.yaw);
            buf.writeByte(msg.headYaw);
            buf.writeShort(msg.velX);
            buf.writeShort(msg.velY);
            buf.writeShort(msg.velZ);
            if (msg.entity instanceof IEntityAdditionalSpawnData)
            {
                ((IEntityAdditionalSpawnData) msg.entity).writeSpawnData(buf);
            }
        }

        public static SpawnEntity decode(PacketByteBuf buf)
        {
            return new SpawnEntity(
                    buf.readVarInt(),
                    buf.readInt(),
                    new UUID(buf.readLong(), buf.readLong()),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readByte(), buf.readByte(), buf.readByte(),
                    buf.readShort(), buf.readShort(), buf.readShort(),
                    buf
                    );
        }

        public static void handle(SpawnEntity msg, Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                EntityType<?> type = Registry.ENTITY_TYPE.get(msg.typeId);
                if (type == null)
                {
                    throw new RuntimeException(String.format("Could not spawn entity (id %d) with unknown type at (%f, %f, %f)", msg.entityId, msg.posX, msg.posY, msg.posZ));
                }

                Optional<World> world = LogicalSidedProvider.CLIENTWORLD.get(ctx.get().getDirection().getReceptionSide());
                Entity e = world.map(w->type.customClientSpawn(msg, w)).orElse(null);
                if (e == null)
                {
                    return;
                }

                e.updateTrackedPosition(msg.posX, msg.posY, msg.posZ);
                e.updatePositionAndAngles(msg.posX, msg.posY, msg.posZ, (msg.yaw * 360) / 256.0F, (msg.pitch * 360) / 256.0F);
                e.setHeadYaw((msg.headYaw * 360) / 256.0F);
                e.setYaw((msg.headYaw * 360) / 256.0F);

                e.setEntityId(msg.entityId);
                e.setUuid(msg.uuid);
                world.filter(ClientWorld.class::isInstance).ifPresent(w->((ClientWorld)w).addEntity(msg.entityId, e));
                e.setVelocityClient(msg.velX / 8000.0, msg.velY / 8000.0, msg.velZ / 8000.0);
                if (e instanceof IEntityAdditionalSpawnData)
                {
                    ((IEntityAdditionalSpawnData) e).readSpawnData(msg.buf);
                }
            });
            ctx.get().setPacketHandled(true);
        }

        public Entity getEntity()
        {
            return entity;
        }

        public int getTypeId()
        {
            return typeId;
        }

        public int getEntityId()
        {
            return entityId;
        }

        public UUID getUuid()
        {
            return uuid;
        }

        public double getPosX()
        {
            return posX;
        }

        public double getPosY()
        {
            return posY;
        }

        public double getPosZ()
        {
            return posZ;
        }

        public byte getPitch()
        {
            return pitch;
        }

        public byte getYaw()
        {
            return yaw;
        }

        public byte getHeadYaw()
        {
            return headYaw;
        }

        public int getVelX()
        {
            return velX;
        }

        public int getVelY()
        {
            return velY;
        }

        public int getVelZ()
        {
            return velZ;
        }

        public PacketByteBuf getAdditionalData()
        {
            return buf;
        }
    }

    public static class OpenContainer
    {
        private final int id;
        private final int windowId;
        private final Text name;
        private final PacketByteBuf additionalData;

        OpenContainer(ContainerType<?> id, int windowId, Text name, PacketByteBuf additionalData)
        {
            this(Registry.CONTAINER.getRawId(id), windowId, name, additionalData);
        }

        private OpenContainer(int id, int windowId, Text name, PacketByteBuf additionalData)
        {
            this.id = id;
            this.windowId = windowId;
            this.name = name;
            this.additionalData = additionalData;
        }

        public static void encode(OpenContainer msg, PacketByteBuf buf)
        {
            buf.writeVarInt(msg.id);
            buf.writeVarInt(msg.windowId);
            buf.writeText(msg.name);
            buf.writeByteArray(msg.additionalData.readByteArray());
        }

        public static OpenContainer decode(PacketByteBuf buf)
        {
            return new OpenContainer(buf.readVarInt(), buf.readVarInt(), buf.readText(), new PacketByteBuf(Unpooled.wrappedBuffer(buf.readByteArray(32600))));
        }

        public static void handle(OpenContainer msg, Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                Screens.getScreenFactory(msg.getType(), MinecraftClient.getInstance(), msg.getWindowId(), msg.getName())
                             .ifPresent(f -> {
                                 Container c = msg.getType().create(msg.getWindowId(), MinecraftClient.getInstance().player.inventory, msg.getAdditionalData());
                                 @SuppressWarnings("unchecked")
                                 Screen s = ((Screens.Provider<Container, ?>)f).create(c, MinecraftClient.getInstance().player.inventory, msg.getName());
                                 MinecraftClient.getInstance().player.container = ((ContainerProvider<?>)s).getContainer();
                                 MinecraftClient.getInstance().openScreen(s);
                             });
            });
            ctx.get().setPacketHandled(true);
        }

        public final ContainerType<?> getType() {
            return Registry.CONTAINER.get(this.id);
        }

        public int getWindowId() {
            return windowId;
        }

        public Text getName() {
            return name;
        }

        public PacketByteBuf getAdditionalData() {
            return additionalData;
        }
    }

    public static class DimensionInfoMessage
    {
        private Identifier dimName;
        private boolean skylight;
        private int id;
        private Identifier modDimensionName;
        private PacketByteBuf extraData;

        DimensionInfoMessage(DimensionType type) {
            id = type.getRawId() + 1;
            dimName = type.getRegistryName();
            modDimensionName = type.getModType().getRegistryName();
            skylight = type.hasSkyLight();
            extraData = new PacketByteBuf(Unpooled.buffer());
            type.getModType().write(extraData, true);
        }

        DimensionInfoMessage(final int dimId, final Identifier dimname, final Identifier modDimensionName, final boolean skylight, final PacketByteBuf extraData) {
            id = dimId;
            this.dimName = dimname;
            this.modDimensionName = modDimensionName;
            this.skylight = skylight;
            this.extraData = extraData;
        }

        public static DimensionInfoMessage decode(PacketByteBuf buffer) {
            int dimId = buffer.readInt();
            Identifier dimname = buffer.readIdentifier();
            Identifier moddimname = buffer.readIdentifier();
            boolean skylight = buffer.readBoolean();
            PacketByteBuf pb = new PacketByteBuf(Unpooled.wrappedBuffer(buffer.readByteArray()));
            return new DimensionInfoMessage(dimId, dimname, moddimname, skylight, pb);
        }

        public static void encode(DimensionInfoMessage message, PacketByteBuf buffer) {
            buffer.writeInt(message.id);
            buffer.writeIdentifier(message.dimName);
            buffer.writeIdentifier(message.modDimensionName);
            buffer.writeBoolean(message.skylight);
            buffer.writeByteArray(message.extraData.array());
        }

        private DimensionType makeDummyDimensionType() {
            final ModDimension modDim = ForgeRegistries.MOD_DIMENSIONS.getValue(modDimensionName);
            // default to overworld if no moddim found
            if (modDim == null) return DimensionType.OVERWORLD;
            modDim.read(extraData, true);
            return new DimensionType(id, "dummy", "dummy", modDim.getFactory(), skylight, VoronoiBiomeAccessType.INSTANCE, modDim, extraData);
        }

        public static boolean handle(final DimensionInfoMessage message, final Supplier<NetworkEvent.Context> contextSupplier) {
            contextSupplier.get().enqueueWork(()-> NetworkHooks.addCachedDimensionType(message.makeDummyDimensionType(), message.dimName));
            return true;
        }
    }
}
