package ru.bolidalarmpro.gui;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import ru.bolidalarmpro.device.DevicePorts;
import ru.bolidalarmpro.device.DeviceTileEntity;
import ru.bolidalarmpro.network.ModPackets;
import ru.bolidalarmpro.sim.DeviceKind;

/** PProg/UProg-like front-panel screen: state, configuration and event journal. */
public final class GuiDevice extends GuiScreen {
    private final EntityPlayer player;
    private final World world;
    private final int x, y, z;
    private final DeviceTileEntity te;
    private int tab = 0;
    private int selectedPort = 0;
    private GuiTextField rsField;
    private GuiTextField dplField;
    private GuiTextField asptField;

    public GuiDevice(EntityPlayer player, World world, int x, int y, int z) {
        this.player = player;
        this.world = world;
        this.x = x; this.y = y; this.z = z;
        TileEntity raw = world.getTileEntity(x, y, z);
        this.te = raw instanceof DeviceTileEntity ? (DeviceTileEntity) raw : null;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(0, 10, 8, 68, 18, "Состояние"));
        buttonList.add(new GuiButton(1, 82, 8, 68, 18, "Конфигурация"));
        buttonList.add(new GuiButton(2, 154, 8, 68, 18, "Журнал"));
        buttonList.add(new GuiButton(99, width - 78, 8, 68, 18, "Закрыть"));
        if (te == null) return;

        if (tab == 0) {
            buttonList.add(new GuiButton(10, 12, height - 52, 74, 18, "СБРОС"));
            buttonList.add(new GuiButton(11, 90, height - 52, 74, 18, "НАЖАТЬ"));
            buttonList.add(new GuiButton(12, 168, height - 52, 74, 18, "АВТОМАТИКА"));
            buttonList.add(new GuiButton(13, 246, height - 52, 74, 18, "РУЧН. ПУСК"));
            buttonList.add(new GuiButton(14, 12, height - 30, 74, 18, "ОТМЕНА"));
            buttonList.add(new GuiButton(15, 90, height - 30, 74, 18, "ДИСТ. ПУСК"));
            buttonList.add(new GuiButton(16, 168, height - 30, 74, 18, "ТЕСТ ЛАМП"));
            buttonList.add(new GuiButton(17, 246, height - 30, 74, 18, "СЕТЬ 220"));
        } else if (tab == 1) {
            rsField = new GuiTextField(fontRendererObj, 18, 58, 40, 16);
            rsField.setMaxStringLength(3);
            rsField.setText("" + te.clientRsAddress);
            dplField = new GuiTextField(fontRendererObj, 18, 92, 40, 16);
            dplField.setMaxStringLength(3);
            dplField.setText("" + te.clientDplAddress);
            asptField = new GuiTextField(fontRendererObj, 18, 126, 40, 16);
            asptField.setMaxStringLength(3);
            asptField.setText("" + te.clientConfiguredAsptAddress);
            buttonList.add(new GuiButton(30, 64, 57, 58, 18, "Адр. RS"));
            buttonList.add(new GuiButton(31, 64, 91, 58, 18, "Адр. ДПЛС"));
            buttonList.add(new GuiButton(32, 64, 125, 58, 18, "АСПТ"));
            buttonList.add(new GuiButton(35, 132, 57, 22, 18, "<"));
            buttonList.add(new GuiButton(36, 156, 57, 22, 18, ">"));
            buttonList.add(new GuiButton(33, 182, 57, 58, 18, "Канал +"));
            buttonList.add(new GuiButton(34, 244, 57, 58, 18, "Канал -"));
            for (int i = 0; i < 5; i++) {
                buttonList.add(new GuiButton(40 + i, 132 + i * 58, 92, 54, 18, "Вых " + (i + 1)));
            }
            buttonList.add(new GuiButton(18, 18, height - 52, 74, 18, "ДВЕРЬ"));
            buttonList.add(new GuiButton(19, 96, height - 52, 74, 18, "ИЗВЕЩ."));
            buttonList.add(new GuiButton(20, 174, height - 52, 74, 18, "24 В ТЕСТ"));
            buttonList.add(new GuiButton(21, 252, height - 52, 74, 18, "АКБ 7"));
            buttonList.add(new GuiButton(22, 18, height - 30, 74, 18, "АКБ 4,5"));
            buttonList.add(new GuiButton(23, 96, height - 30, 74, 18, "ПРИОР."));
            buttonList.add(new GuiButton(24, 174, height - 30, 74, 18, "АВТОВОЗВР."));
            buttonList.add(new GuiButton(25, 252, height - 30, 74, 18, "НОВ. ПИРО"));
            buttonList.add(new GuiButton(26, 330, height - 30, 74, 18, "СНЯТЬ ПОВР."));
        }
    }

    @Override
    public void drawScreen(int mx, int my, float partial) {
        drawDefaultBackground();
        drawRect(8, 8, width - 8, height - 8, 0xC0101010);
        drawRect(10, 30, width - 10, height - 10, 0xFF14181B);
        String title = te == null ? "НЕТ ПРИБОРА" : te.kind.displayName() + "  /  ПО v" + te.kind.firmware();
        drawCenteredString(fontRendererObj, title, width / 2, 14, 0xFFFFFF);
        super.drawScreen(mx, my, partial);
        if (te == null) return;

        if (tab == 0) {
            drawRect(14, 38, width - 14, 62, 0xFF203020);
            fontRendererObj.drawString("ЖКИ: " + te.clientLcd0, 20, 43, 0x66FF66);
            fontRendererObj.drawString("      " + te.clientLcd1, 20, 53, 0x66FF66);
            String[] status = te.clientStatusText.split("\n");
            int max = Math.min(status.length, 14);
            for (int i = 0; i < max; i++) {
                fontRendererObj.drawString(status[i], 16, 70 + i * 11, 0xDDDDDD);
            }
            String led = "LED: ПИТ " + on(te.clientLeds[0]) + "  ТРЕВОГА " + on(te.clientLeds[1])
                    + "  НЕИСПР " + on(te.clientLeds[2]) + "  СВЯЗЬ " + on(te.clientLeds[3]);
            fontRendererObj.drawString(led, 16, height - 74, 0xAAAAAA);
            fontRendererObj.drawString("U=" + one(te.clientVoltage) + " В  I=" + one(te.clientCurrent)
                    + " А  АКБ=" + (int) Math.round(te.clientBatterySoc * 100.0) + "%  RS-485="
                    + te.clientRsQualityPercent + "%", 16, height - 62, 0xAAAAAA);
        } else if (tab == 1) {
            fontRendererObj.drawString("Адрес RS-485 (1..127)", 18, 46, 0xAAAAAA);
            fontRendererObj.drawString("Адрес ДПЛС (1..127)", 18, 80, 0xAAAAAA);
            fontRendererObj.drawString("Адрес АСПТ в тактике", 18, 114, 0xAAAAAA);
            if (rsField != null) rsField.drawTextBox();
            if (dplField != null) dplField.drawTextBox();
            if (asptField != null) asptField.drawTextBox();
            List<String> roles = DevicePorts.roles(te.kind);
            if (roles.isEmpty()) {
                fontRendererObj.drawString("Портов нет", 132, 46, 0xAAAAAA);
            } else {
                if (selectedPort < 0) selectedPort = 0;
                if (selectedPort >= roles.size()) selectedPort = roles.size() - 1;
                String role = roles.get(selectedPort);
                Integer channel = te.channels.get(role);
                int ch = channel == null ? 0 : channel.intValue();
                fontRendererObj.drawString("Порт: " + DevicePorts.roleLabel(role) + " / канал " + ch,
                        132, 46, 0x66CCFF);
                fontRendererObj.drawString("Кабели одного типа+цвета = одна сеть", 132, 80, 0x888888);
            }
            fontRendererObj.drawString("Включённые контролируемые выходы АСПТ", 132, 114, 0xAAAAAA);
        } else {
            String[] journal = te.clientLogText.split("\n");
            int max = Math.min(journal.length, 18);
            for (int i = 0; i < max; i++) {
                fontRendererObj.drawString(journal[i], 18, 44 + i * 12, 0xDDDDDD);
            }
            if (journal.length == 0 || (journal.length == 1 && journal[0].length() == 0)) {
                fontRendererObj.drawString("Журнал событий пуст", 18, 44, 0x888888);
            }
        }
        if (te.clientFault.length() > 0) {
            fontRendererObj.drawString("! " + te.clientFault, 16, height - 16, 0xFF5555);
        }
    }

    private static String on(boolean b) { return b ? "ВКЛ" : "---"; }

    private static String one(double v) {
        long x = Math.round(v * 10.0);
        return "" + (x / 10.0);
    }

    @Override
    public void actionPerformed(GuiButton b) {
        if (b.id == 0 || b.id == 1 || b.id == 2) {
            tab = b.id;
            initGui();
            return;
        }
        if (b.id == 99) {
            if (mc != null && mc.thePlayer != null) mc.thePlayer.closeScreen();
            return;
        }
        if (te == null) return;
        switch (b.id) {
            case 10: send(ModPackets.ACT_RESET, 0, 0); break;
            case 11: send(ModPackets.ACT_PRESS, 0, 0); break;
            case 12: send(ModPackets.ACT_TOGGLE_AUTO, 0, 0); break;
            case 13: send(ModPackets.ACT_LOCAL_START, 0, 0); break;
            case 14: send(ModPackets.ACT_CANCEL, 0, 0); break;
            case 15: send(ModPackets.ACT_TOGGLE_REMOTE_START, 0, 0); break;
            case 16: send(ModPackets.ACT_TEST_INDICATION, 0, 0); break;
            case 17: send(ModPackets.ACT_TOGGLE_MAINS, 0, 0); break;
            case 18: send(ModPackets.ACT_TOGGLE_DOOR, 0, 0); break;
            case 19: send(ModPackets.ACT_TOGGLE_SENSOR, 0, 0); break;
            case 20: send(ModPackets.ACT_TOGGLE_24V_TEST, 0, 0); break;
            case 21: send(ModPackets.ACT_TOGGLE_BATTERY7, 0, 0); break;
            case 22: send(ModPackets.ACT_TOGGLE_BATTERY45, 0, 0); break;
            case 23: send(ModPackets.ACT_TOGGLE_REMOTE_PRIORITY, 0, 0); break;
            case 24: send(ModPackets.ACT_TOGGLE_AUTO_RESTORE, 0, 0); break;
            case 25: send(ModPackets.ACT_REPLACE_PYRO, 0, 0); break;
            case 26: send(ModPackets.ACT_RESET_DAMAGE, 0, 0); break;
            case 30: send(ModPackets.ACT_SET_RS_ADDRESS, parseInt(rsField, 127), 0); break;
            case 31: send(ModPackets.ACT_SET_DPL_ADDRESS, parseInt(dplField, 1), 0); break;
            case 32: send(ModPackets.ACT_SET_ASPT_ADDRESS, parseInt(asptField, 2), 0); break;
            case 33: changeChannel(1); break;
            case 34: changeChannel(-1); break;
            case 35: selectedPort--; initGui(); break;
            case 36: selectedPort++; initGui(); break;
            default:
                if (b.id >= 40 && b.id <= 44) send(ModPackets.ACT_TOGGLE_OUTPUT, b.id - 40, 0);
                break;
        }
    }

    private void changeChannel(int delta) {
        List<String> roles = DevicePorts.roles(te.kind);
        if (roles.isEmpty()) return;
        if (selectedPort < 0) selectedPort = 0;
        if (selectedPort >= roles.size()) selectedPort = roles.size() - 1;
        String role = roles.get(selectedPort);
        Integer channel = te.channels.get(role);
        int ch = channel == null ? 0 : channel.intValue();
        ch = (ch + delta + 16) & 15;
        send(ModPackets.ACT_SET_CHANNEL, selectedPort, ch);
        initGui();
    }

    private static int parseInt(GuiTextField field, int fallback) {
        if (field == null) return fallback;
        try {
            return Integer.parseInt(field.getText());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void send(int action, int a, int b) {
        ModPackets.sendAction(x, y, z, action, a, b);
    }

    @Override
    public void keyTyped(char c, int key) {
        if (rsField != null && rsField.textboxKeyTyped(c, key)) return;
        if (dplField != null && dplField.textboxKeyTyped(c, key)) return;
        if (asptField != null && asptField.textboxKeyTyped(c, key)) return;
        super.keyTyped(c, key);
    }

    @Override
    public void mouseClicked(int mx, int my, int button) {
        if (rsField != null) rsField.mouseClicked(mx, my, button);
        if (dplField != null) dplField.mouseClicked(mx, my, button);
        if (asptField != null) asptField.mouseClicked(mx, my, button);
        super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
