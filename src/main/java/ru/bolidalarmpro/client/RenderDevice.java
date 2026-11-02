package ru.bolidalarmpro.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.bolidalarmpro.ModInfo;
import ru.bolidalarmpro.device.DeviceTileEntity;
import ru.bolidalarmpro.sim.DeviceKind;

/** Thin wall-mounted 3D case with a real front-panel texture, live LEDs and a 2x16 LCD. */
public final class RenderDevice extends TileEntitySpecialRenderer {
    private static final float U1 = 0.0625F;
    private static final float V1 = 0.0625F;
    private static final float U2 = 0.9375F;
    private static final float V2 = 0.9375F;
    private static final float BU1 = 0.005F;
    private static final float BV1 = 0.005F;
    private static final float BU2 = 0.055F;
    private static final float BV2 = 0.055F;

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) {
        if (!(te instanceof DeviceTileEntity)) return;
        DeviceTileEntity d = (DeviceTileEntity) te;
        DeviceKind kind = d.kind;

        String textureName;
        if (kind.isTablo()) {
            textureName = "device_" + kind.texture() + (d.clientLit ? "_on" : "_off") + ".png";
        } else {
            textureName = "device_" + kind.texture() + ".png";
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(
                new ResourceLocation(ModInfo.MODID, "textures/blocks/" + textureName));

        float w = 0.72F;
        float h = 0.66F;
        float dep = 0.12F;
        if (kind == DeviceKind.RIP_12 || kind == DeviceKind.ASPT_353) {
            w = 0.88F; h = 0.88F; dep = 0.22F;
        } else if (kind == DeviceKind.KDL_236) {
            w = 0.80F; h = 0.58F; dep = 0.10F;
        } else if (kind.isAddressable()) {
            w = 0.56F; h = 0.56F; dep = 0.12F;
        } else if (kind.isTablo()) {
            w = 0.94F; h = 0.32F; dep = 0.055F;
        } else if (kind.isPyro()) {
            w = 0.30F; h = 0.52F; dep = 0.30F;
        } else if (kind.isInfrastructure()) {
            w = 0.50F; h = 0.50F; dep = 0.12F;
        }

        float angle = 0.0F;
        if (d.clientFacing == 3) angle = 180.0F;
        else if (d.clientFacing == 4) angle = 90.0F;
        else if (d.clientFacing == 5) angle = -90.0F;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
        GL11.glRotatef(angle, 0.0F, 1.0F, 0.0F);

        float x1 = -w / 2.0F, x2 = w / 2.0F;
        float y1 = -h / 2.0F, y2 = h / 2.0F;
        float z1 = -dep / 2.0F, z2 = dep / 2.0F;
        Tessellator t = Tessellator.instance;
        drawBox(t, x1, y1, z1, x2, y2, z2, kind.isTwoSidedTablo());

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (!kind.isTablo() && !kind.isPyro()) {
            drawLed(t, x1, y1, w, h, z2 + 0.003F, 0.18F, 0.18F, 0.035F, d.clientLeds[0], 0.15F, 1.0F, 0.15F);
            drawLed(t, x1, y1, w, h, z2 + 0.003F, 0.72F, 0.18F, 0.035F, d.clientLeds[1], 1.0F, 0.12F, 0.08F);
            drawLed(t, x1, y1, w, h, z2 + 0.003F, 0.18F, 0.82F, 0.035F, d.clientLeds[2], 1.0F, 0.75F, 0.08F);
            drawLed(t, x1, y1, w, h, z2 + 0.003F, 0.72F, 0.82F, 0.035F, d.clientLeds[3], 0.20F, 0.55F, 1.0F);
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        if (kind.isPanel()) {
            FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
            if (font != null) drawLcdText(font, x1, y2, z2, d.clientLcd0, d.clientLcd1);
        }

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    private static void drawBox(Tessellator t, float x1, float y1, float z1,
                                float x2, float y2, float z2, boolean twoSided) {
        t.startDrawingQuads();
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        t.setBrightness(0xF000F0);
        // Front: the photo-like panel artwork.
        quad(t, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
                U1, V1, U2, V2, 0.0F, 0.0F, 1.0F);
        if (twoSided) {
            // Back of a Молния-2 sign carries the same legend.
            quad(t, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1,
                    U1, V1, U2, V2, 0.0F, 0.0F, -1.0F);
        } else {
            quad(t, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1,
                    BU1, BV1, BU2, BV2, 0.0F, 0.0F, -1.0F);
        }
        // Sides sample the solid-colour border of the same texture.
        quad(t, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
                BU1, BV1, BU2, BV2, -1.0F, 0.0F, 0.0F);
        quad(t, x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2,
                BU1, BV1, BU2, BV2, 1.0F, 0.0F, 0.0F);
        quad(t, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1,
                BU1, BV1, BU2, BV2, 0.0F, 1.0F, 0.0F);
        quad(t, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
                BU1, BV1, BU2, BV2, 0.0F, -1.0F, 0.0F);
        t.draw();
    }

    private static void quad(Tessellator t,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz,
                             float u1, float v1, float u2, float v2,
                             float nx, float ny, float nz) {
        t.setNormal(nx, ny, nz);
        t.addVertexWithUV(ax, ay, az, u1, v2);
        t.addVertexWithUV(bx, by, bz, u2, v2);
        t.addVertexWithUV(cx, cy, cz, u2, v1);
        t.addVertexWithUV(dx, dy, dz, u1, v1);
    }

    private static void drawLed(Tessellator t, float x1, float y1, float w, float h,
                                float z, float u, float v, float size,
                                boolean on, float r, float g, float b) {
        float lx = x1 + u * w;
        float ly = y1 + h - v * h;
        t.startDrawingQuads();
        t.setColorOpaque_F(on ? r : 0.06F, on ? g : 0.06F, on ? b : 0.06F);
        t.addVertexWithUV(lx, ly, z, 0.0F, 0.0F);
        t.addVertexWithUV(lx + size, ly, z, 0.0F, 0.0F);
        t.addVertexWithUV(lx + size, ly - size, z, 0.0F, 0.0F);
        t.addVertexWithUV(lx, ly - size, z, 0.0F, 0.0F);
        t.draw();
    }

    private static void drawLcdText(FontRenderer font, float x1, float y2, float z2,
                                    String line1, String line2) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x1 + 0.055F, y2 - 0.055F, z2 + 0.004F);
        GL11.glScalef(0.006F, -0.006F, 0.006F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        font.drawString(line1 == null ? "" : line1, 0, 0, 0x55FF55);
        font.drawString(line2 == null ? "" : line2, 0, 12, 0x55FF55);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}
