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

package net.minecraftforge.client.event;

import net.minecraft.network.MessageType;
import net.minecraft.text.Text;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@net.minecraftforge.eventbus.api.Cancelable
public class ClientChatReceivedEvent extends net.minecraftforge.eventbus.api.Event
{
    private Text message;
    private final MessageType type;
    public ClientChatReceivedEvent(MessageType type, Text message)
    {
        this.type = type;
        this.setMessage(message);
    }

    public Text getMessage()
    {
        return message;
    }

    public void setMessage(Text message)
    {
        this.message = message;
    }

    public MessageType getType()
    {
        return type;
    }
}
