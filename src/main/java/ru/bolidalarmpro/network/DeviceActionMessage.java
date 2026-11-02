package ru.bolidalarmpro.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import ru.bolidalarmpro.device.DeviceTileEntity;

public final class DeviceActionMessage implements IMessage {
    public int x, y, z;
    public int action, a, b;

    public DeviceActionMessage() {}

    public DeviceActionMessage(int x, int y, int z, int action, int a, int b) {
        this.x = x; this.y = y; this.z = z; this.action = action; this.a = a; this.b = b;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z);
        buf.writeInt(action); buf.writeInt(a); buf.writeInt(b);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt();
        action = buf.readInt(); a = buf.readInt(); b = buf.readInt();
    }

    public static final class Handler implements IMessageHandler<DeviceActionMessage, IMessage> {
        @Override
        public IMessage onMessage(DeviceActionMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) return null;
            World world = player.worldObj;
            TileEntity te = world.getTileEntity(message.x, message.y, message.z);
            if (te instanceof DeviceTileEntity) {
                ((DeviceTileEntity) te).handleAction(message.action, message.a, message.b);
            }
            return null;
        }
    }
}
