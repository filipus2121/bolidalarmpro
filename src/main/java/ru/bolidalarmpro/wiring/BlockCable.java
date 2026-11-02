package ru.bolidalarmpro.wiring;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import ru.bolidalarmpro.ModCreativeTab;
import ru.bolidalarmpro.ModInfo;
import ru.bolidalarmpro.ModItems;
import ru.bolidalarmpro.sim.CableFlags;
import ru.bolidalarmpro.sim.Pos;
import ru.bolidalarmpro.sim.WireKind;

/** A thin coloured cable pad. Metadata 0..15 is the independent channel of this cable class. */
public final class BlockCable extends Block {
    public final WireKind kind;
    private final IIcon[] icons = new IIcon[16];

    public BlockCable(WireKind kind) {
        super(Material.circuits);
        this.kind = kind;
        setBlockName(ModInfo.MODID + ".cable." + kind.id());
        setCreativeTab(ModCreativeTab.TAB);
        setHardness(0.1F);
        setStepSound(Block.soundTypeStone);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.0625F, 1.0F);
    }

    @Override
    public boolean isOpaqueCube() { return false; }

    @Override
    public boolean renderAsNormalBlock() { return false; }

    @Override
    public int getRenderType() { return 0; }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
        return null;
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World w, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox((double) x, (double) y, (double) z,
                (double) x + 1.0, (double) y + 0.0625, (double) z + 1.0);
    }

    @Override
    public void registerBlockIcons(IIconRegister reg) {
        for (int i = 0; i < 16; i++) {
            icons[i] = reg.registerIcon(ModInfo.RES + "cable_" + kind.id() + "_" + i);
        }
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return icons[meta & 15];
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < 16; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    public int damageDropped(int meta) {
        return meta & 15;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        WiringWorldData.get(world).addCable(new Pos(x, y, z));
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        WiringWorldData.get(world).removeCable(new Pos(x, y, z));
        super.breakBlock(world, x, y, z, block, meta);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        WiringWorldData.get(world).markTopologyDirty();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
                                    int side, float hx, float hy, float hz) {
        if (world.isRemote) return true;
        WiringWorldData data = WiringWorldData.get(world);
        Pos p = new Pos(x, y, z);
        ItemStack held = player.getHeldItem();
        int channel = world.getBlockMetadata(x, y, z) & 15;

        if (ModItems.isHeld(held, ModItems.programmer)) {
            int next = (channel + 1) & 15;
            world.setBlockMetadataWithNotify(x, y, z, next, 3);
            data.markTopologyDirty();
            chat(player, kind.label() + ": канал " + next + " / 16");
            return true;
        }
        if (ModItems.isHeld(held, ModItems.multimeter)) {
            chat(player, SimulationHost.get(world).describeNetAt(p, kind, channel));
            return true;
        }
        CableFlags f = data.getFlags(p);
        if (ModItems.isHeld(held, ModItems.shortJumper)) {
            f.shortFault = !f.shortFault;
            chat(player, f.shortFault ? "ВНЕСЕНО КЗ ЛИНИИ" : "КЗ ЛИНИИ УСТРАНЕНО");
        } else if (ModItems.isHeld(held, ModItems.eolResistor)) {
            f.eol = !f.eol;
            chat(player, f.eol ? "УСТАНОВЛЕН ОКОНЕЧНЫЙ РЕЗИСТОР 4,7 кОм" : "ОКОНЕЧНЫЙ РЕЗИСТОР СНЯТ");
        } else if (ModItems.isHeld(held, ModItems.terminator)) {
            f.terminator = !f.terminator;
            chat(player, f.terminator ? "УСТАНОВЛЕН ТЕРМИНАТОР 120 ОМ" : "ТЕРМИНАТОР 120 ОМ СНЯТ");
        } else {
            chat(player, "Кабель " + kind.label() + " / канал " + channel
                    + ". Инструменты: программатор, мультиметр, КЗ, EOL, 120 Ом");
            return true;
        }
        data.setFlags(p, f);
        return true;
    }

    private static void chat(EntityPlayer player, String text) {
        player.addChatComponentMessage(new ChatComponentText(text));
    }
}
