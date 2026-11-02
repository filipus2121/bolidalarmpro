package ru.bolidalarmpro.network;

import java.util.Random;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

public final class PowderEffectMessage implements IMessage {
    public int x, y, z;

    public PowderEffectMessage() {}
    public PowderEffectMessage(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt();
    }

    @SideOnly(Side.CLIENT)
    public static final class Handler implements IMessageHandler<PowderEffectMessage, IMessage> {
        @Override
        public IMessage onMessage(PowderEffectMessage message, MessageContext ctx) {
            World world = Minecraft.getMinecraft().theWorld;
            if (world == null || !world.isRemote) return null;
            Random random = new Random();
            for (int i = 0; i < 90; i++) {
                double px = message.x + 0.5 + (random.nextDouble() - 0.5) * 5.0;
                double py = message.y + 0.4 + random.nextDouble() * 2.5;
                double pz = message.z + 0.5 + (random.nextDouble() - 0.5) * 5.0;
                world.spawnParticle("largesmoke", px, py, pz, 0.0, 0.02, 0.0);
                world.spawnParticle("townaura", px, py, pz, 0.0, 0.01, 0.0);
            }
            return null;
        }
    }
}
