#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate all 1.7.10 textures for BOLID Alarm Pro.

Requires Pillow and a Cyrillic TTF font (DejaVu Sans is used when available).
The PNGs written here are committed, so players do not need Python to run the mod.
"""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "bolidalarmpro", "textures")
BLOCKS = os.path.join(ROOT, "blocks")
ITEMS = os.path.join(ROOT, "items")
GUI = os.path.join(ROOT, "gui")
for p in (BLOCKS, ITEMS, GUI):
    os.makedirs(p, exist_ok=True)

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

def font(size, bold=True):
    path = FONT_BOLD if bold else FONT_REG
    if os.path.exists(path):
        return ImageFont.truetype(path, size)
    return ImageFont.load_default()

def centered(d, text, y, fnt, fill, cx=128):
    try:
        box = d.textbbox((0, 0), text, font=fnt)
        w = box[2] - box[0]
    except Exception:
        w = len(text) * fnt.size // 2
    d.text((cx - w / 2.0, y), text, font=fnt, fill=fill)

def base(body, panel_bg, outline=(35, 38, 40)):
    img = Image.new("RGB", (256, 256), body)
    d = ImageDraw.Draw(img)
    # 16 px solid border: the TESR uses this region for the sides of the case.
    for yy in range(16, 240):
        shade = int(panel_bg[0] * 0.96 + (yy - 16) * 0.04)
        d.line((16, yy, 239, yy), fill=(max(0, min(255, shade)),
                                        max(0, min(255, shade + 2)),
                                        max(0, min(255, shade + 4))))
    d.rectangle((16, 16, 239, 239), outline=outline, width=2)
    for sx, sy in ((24, 24), (232, 24), (24, 232), (232, 232)):
        d.ellipse((sx - 4, sy - 4, sx + 4, sy + 4), fill=(150, 150, 150), outline=(70, 70, 70))
        d.line((sx - 2, sy, sx + 2, sy), fill=(80, 80, 80))
    return img, d

def save(img, name):
    img.save(os.path.join(BLOCKS, name), "PNG", optimize=True)

def leds(d, positions, colors, radius=6):
    for (x, y), col in zip(positions, colors):
        d.ellipse((x - radius, y - radius, x + radius, y + radius),
                  fill=col, outline=(30, 30, 30), width=2)
        d.ellipse((x - 2, y - 2, x + 1, y + 1), fill=(255, 255, 255))

def keypad(d, x0, y0, cols=4, rows=4):
    labels = [["1", "2", "3", "F1"], ["4", "5", "6", "F2"],
              ["7", "8", "9", "F3"], ["*", "0", "#", "F4"]]
    cw, ch, gap = 34, 24, 6
    f = font(12)
    for r in range(rows):
        for c in range(cols):
            x = x0 + c * (cw + gap)
            y = y0 + r * (ch + gap)
            d.rounded_rectangle((x, y, x + cw, y + ch), radius=5,
                                fill=(232, 234, 236), outline=(120, 124, 128), width=1)
            d.text((x + 11, y + 5), labels[r][c], font=f, fill=(30, 35, 40))

# 1. РИП-12 исп.02: white metal cabinet, narrow LED strip, lock.
img, d = base((222, 224, 224), (236, 237, 237))
d.rounded_rectangle((34, 44, 222, 78), radius=3, fill=(65, 70, 72), outline=(35, 38, 40))
centered(d, "РИП-12", 51, font(18), (235, 235, 235), 128)
leds(d, [(60, 62), (82, 62), (104, 62)], [(210, 45, 40), (70, 200, 70), (235, 190, 50)], 5)
d.text((140, 52), "13,6 В  2 А", font=font(13, False), fill=(210, 215, 215))
d.ellipse((190, 140, 210, 160), fill=(190, 192, 194), outline=(90, 92, 94), width=2)
d.line((200, 145, 200, 155), fill=(60, 60, 60), width=2)
centered(d, "РЕЗЕРВИРОВАННЫЙ ИСТОЧНИК ПИТАНИЯ", 200, font(11, False), (100, 104, 106))
centered(d, "БОЛИД  •  АКБ 12 В 7 А·ч", 218, font(12), (90, 94, 96))
save(img, "device_rip12.png")

# 2. С2000М: dark LCD hood + white keypad body.
def s2000m(name, fw, dual):
    img, d = base((224, 226, 226), (242, 242, 238))
    d.polygon([(24, 28), (232, 28), (222, 104), (34, 104)], fill=(18, 24, 34))
    d.rounded_rectangle((44, 38, 212, 88), radius=4, fill=(92, 150, 62), outline=(10, 18, 12), width=2)
    d.text((52, 43), "ДЕЖУРНЫЙ РЕЖИМ", font=font(13), fill=(12, 40, 12))
    d.text((52, 63), "ЗОН 2048  РАЗД 511", font=font(11, False), fill=(12, 40, 12))
    for i, col in enumerate([(220, 40, 40)] * 4 + [(235, 190, 40)] * 4):
        d.ellipse((48 + i * 20, 92, 54 + i * 20, 98), fill=col, outline=(20, 20, 20))
    d.rounded_rectangle((24, 108, 232, 232), radius=18, fill=(244, 244, 240), outline=(180, 182, 184))
    keypad(d, 52, 122)
    d.text((196, 126), "◀", font=font(16), fill=(40, 45, 50))
    d.text((196, 154), "▶", font=font(16), fill=(40, 45, 50))
    d.text((196, 182), "СБРОС", font=font(10), fill=(40, 45, 50))
    centered(d, "С2000М " + ("исп.02" if dual else "") + "   ПО v" + fw, 214, font(12), (70, 74, 78))
    return img
save(s2000m("s2000m205", "2.05", False), "device_s2000m205.png")
save(s2000m("s2000m502", "5.02", True), "device_s2000m502.png")

# 3. С2000-КДЛ: white controller with three LEDs and terminal diagrams.
img, d = base((238, 239, 239), (246, 246, 244))
d.rounded_rectangle((36, 38, 220, 80), radius=4, fill=(248, 248, 246), outline=(120, 124, 126))
centered(d, "С2000-КДЛ", 45, font(19), (25, 28, 30), 128)
centered(d, "Контроллер двухпроводной линии  •  РЭ v2.36", 67, font(10, False), (70, 74, 78))
leds(d, [(72, 100), (128, 100), (184, 100)], [(60, 200, 70), (60, 160, 220), (235, 180, 40)], 6)
d.text((44, 114), "РАБОТА", font=font(9, False), fill=(60, 64, 66))
d.text((102, 114), "RS-485", font=font(9, False), fill=(60, 64, 66))
d.text((160, 114), "ДПЛС", font=font(9, False), fill=(60, 64, 66))
for x in (50, 150):
    d.line((x, 148, x + 56, 148), fill=(35, 38, 40), width=4)
    d.line((x + 56, 148, x + 56, 164), fill=(35, 38, 40), width=4)
    d.line((x + 56, 164, x + 72, 164), fill=(35, 38, 40), width=4)
    d.line((x + 72, 164, x + 72, 148), fill=(35, 38, 40), width=4)
    d.line((x + 72, 148, x + 96, 148), fill=(35, 38, 40), width=4)
centered(d, "ДПЛС 8–11 В  •  до 127 АУ  •  кольцо, контроль КЗ/обрыва", 198, font(11, False), (80, 84, 86))
save(img, "device_kdl236.png")

# 4. С2000-АСПТ: red cabinet, orange legend, 27 LED rows.
img, d = base((196, 28, 24), (214, 34, 28))
d.rounded_rectangle((30, 32, 226, 132), radius=3, fill=(245, 226, 190), outline=(100, 60, 30), width=2)
centered(d, "С2000-АСПТ  •  v3.53", 38, font(14), (60, 35, 20))
rows = ["СЕТЬ", "АКБ", "АВТОМАТИКА", "ПОЖАР", "ЗАДЕРЖКА", "ЗАПУСК", "ТУШЕНИЕ", "НЕИСПР."]
for i, label in enumerate(rows):
    yy = 60 + i * 9
    d.text((38, yy), label, font=font(8, False), fill=(70, 45, 25))
    for j in range(3):
        col = (210, 45, 35) if (i + j) % 3 == 0 else (235, 175, 55)
        d.ellipse((112 + j * 10, yy + 1, 118 + j * 10, yy + 7), fill=col, outline=(90, 60, 30))
d.text((160, 62), "ШС1..ШС3", font=font(9, False), fill=(70, 45, 25))
d.text((160, 76), "ДВЕРЬ", font=font(9, False), fill=(70, 45, 25))
d.text((160, 90), "ОТВ", font=font(9, False), fill=(70, 45, 25))
d.text((160, 104), "СО1/СО2/ЗО", font=font(9, False), fill=(70, 45, 25))
d.ellipse((200, 104, 216, 120), fill=(190, 192, 194), outline=(80, 40, 30), width=2)
d.line((208, 108, 208, 116), fill=(40, 30, 25), width=2)
centered(d, "АВТОМАТИЧЕСКАЯ СТАНЦИЯ ПОРОШКОВОГО ПОЖАРОТУШЕНИЯ", 154, font(11), (255, 220, 210))
centered(d, "2 x 12 В 4,5 А·ч   •   24 В / 1 А   •   задержка 15 / 30 с", 180, font(11, False), (255, 210, 200))
d.line((40, 210, 216, 210), fill=(150, 20, 16), width=3)
save(img, "device_aspt353.png")

# 5. ИПР 513-3АМ: red manual call point with glass and white push plate.
img, d = base((205, 28, 24), (222, 34, 28))
d.rounded_rectangle((30, 30, 226, 226), radius=14, fill=(218, 32, 26), outline=(120, 18, 14), width=3)
d.rounded_rectangle((38, 38, 218, 78), radius=5, fill=(235, 235, 225), outline=(160, 160, 150))
d.polygon([(112, 44), (112, 54), (102, 54), (102, 68), (122, 68), (122, 54), (116, 54)], outline=(45, 45, 45))
d.line((102, 68, 102, 74), fill=(45, 45, 45), width=2)
d.ellipse((188, 50, 196, 58), fill=(60, 45, 40), outline=(20, 15, 15))
d.rounded_rectangle((42, 96, 214, 200), radius=6, fill=(245, 244, 235), outline=(150, 150, 145), width=2)
d.polygon([(66, 146), (90, 146), (90, 136), (108, 152), (90, 168), (90, 158), (66, 158)], fill=(30, 32, 34))
d.ellipse((116, 136, 140, 164), fill=(25, 25, 25))
d.polygon([(190, 146), (166, 146), (166, 136), (148, 152), (166, 168), (166, 158), (190, 158)], fill=(30, 32, 34))
centered(d, "ИПР 513-3АМ", 208, font(13), (255, 230, 225))
save(img, "device_ipr513.png")

# 6. УДП 513-3АМ: yellow remote-start device.
img, d = base((240, 205, 35), (248, 214, 45))
d.rounded_rectangle((30, 30, 226, 226), radius=14, fill=(247, 211, 42), outline=(170, 135, 20), width=3)
d.rounded_rectangle((38, 38, 218, 82), radius=5, fill=(238, 238, 220), outline=(170, 170, 150))
centered(d, "ПУСК ПОЖАРОТУШЕНИЯ", 48, font(15), (25, 25, 25))
d.ellipse((188, 42, 196, 54), fill=(90, 75, 45), outline=(40, 35, 25))
d.rounded_rectangle((42, 96, 214, 200), radius=6, fill=(245, 244, 232), outline=(160, 155, 130), width=2)
d.polygon([(66, 146), (90, 146), (90, 136), (108, 152), (90, 168), (90, 158), (66, 158)], fill=(35, 35, 35))
d.ellipse((116, 136, 140, 164), fill=(45, 40, 35))
d.polygon([(190, 146), (166, 146), (166, 136), (148, 152), (166, 168), (166, 158), (190, 158)], fill=(35, 35, 35))
centered(d, "УДП 513-3АМ  •  БРИЗ", 208, font(12), (80, 60, 20))
save(img, "device_udp513.png")

# 7. Пиропатрон порошковый: metal cartridge with two leads.
img, d = base((58, 60, 62), (78, 80, 82))
d.rounded_rectangle((92, 46, 164, 196), radius=12, fill=(178, 180, 182), outline=(55, 55, 55), width=3)
d.rectangle((92, 70, 164, 96), fill=(210, 170, 60), outline=(80, 60, 20))
centered(d, "ПП", 72, font(16), (40, 30, 20))
d.rectangle((104, 110, 152, 176), fill=(205, 70, 45), outline=(90, 30, 20))
centered(d, "ПОРОШОК", 128, font(11), (255, 235, 225))
d.line((104, 196, 92, 226), fill=(190, 40, 35), width=3)
d.line((152, 196, 164, 226), fill=(35, 45, 190), width=3)
centered(d, "ОДНОРАЗОВЫЙ  •  0,5 А", 216, font(11, False), (220, 220, 220))
save(img, "device_pyro.png")

# 8. Молния2-12 tablo, off/on for both legends.
def tablo(legend, on, name):
    img = Image.new("RGB", (256, 256), (238, 239, 239))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((8, 72, 248, 184), radius=10, fill=(248, 248, 246), outline=(170, 172, 174), width=3)
    d.ellipse((18, 122, 28, 132), fill=(210, 212, 214), outline=(140, 142, 144))
    d.ellipse((228, 122, 238, 132), fill=(210, 212, 214), outline=(140, 142, 144))
    if on:
        glow = Image.new("RGB", (256, 256), (0, 0, 0))
        gd = ImageDraw.Draw(glow)
        gd.rounded_rectangle((28, 86, 228, 170), radius=6, fill=(240, 30, 25))
        glow = glow.filter(ImageFilter.GaussianBlur(12))
        img = Image.blend(img, glow, 0.35)
        d = ImageDraw.Draw(img)
    fill = (226, 32, 28) if on else (122, 30, 28)
    d.rounded_rectangle((28, 86, 228, 170), radius=6, fill=fill, outline=(90, 20, 18), width=2)
    txtfill = (255, 255, 255) if on else (210, 190, 188)
    centered(d, "ПОРОШОК!", 98, font(26), txtfill)
    centered(d, legend, 132, font(30), txtfill)
    return img
save(tablo("НЕ ВХОДИ!", False, "ne"), "device_tablo_ne_off.png")
save(tablo("НЕ ВХОДИ!", True, "ne"), "device_tablo_ne_on.png")
save(tablo("УХОДИ!", False, "uh"), "device_tablo_uh_off.png")
save(tablo("УХОДИ!", True, "uh"), "device_tablo_uh_on.png")

# 9. Laboratory infrastructure.
img, d = base((205, 208, 210), (226, 228, 230))
d.rounded_rectangle((52, 52, 204, 204), radius=8, fill=(242, 242, 240), outline=(110, 112, 114), width=2)
centered(d, "~ 220 В", 86, font(24), (35, 38, 40))
centered(d, "50 Гц  •  СТЕНДОВЫЙ ВВОД", 122, font(12, False), (70, 74, 76))
d.rounded_rectangle((96, 150, 160, 178), radius=4, fill=(195, 45, 40), outline=(80, 25, 20))
centered(d, "ВКЛ", 156, font(13), (255, 235, 230))
save(img, "device_mains220.png")

img, d = base((232, 233, 234), (246, 246, 244))
d.rounded_rectangle((42, 44, 214, 212), radius=8, fill=(250, 250, 248), outline=(120, 124, 126), width=2)
centered(d, "РЕЛЕ 24 В", 58, font(18), (30, 34, 36))
d.rectangle((62, 92, 110, 140), fill=(70, 130, 210), outline=(30, 50, 90))
centered(d, "K1", 104, font(14), (255, 255, 255), 86)
d.rectangle((146, 92, 194, 140), fill=(70, 130, 210), outline=(30, 50, 90))
centered(d, "K2", 104, font(14), (255, 255, 255), 170)
d.text((58, 154), "COM", font=font(11), fill=(40, 44, 46))
d.text((120, 154), "NO1", font=font(11), fill=(40, 44, 46))
d.text((178, 154), "NO2", font=font(11), fill=(40, 44, 46))
centered(d, "СУХИЕ КОНТАКТЫ ДЛЯ ТАБЛО 12 В", 190, font(11, False), (80, 84, 86))
save(img, "device_relay12.png")

img, d = base((235, 236, 236), (248, 248, 246))
d.ellipse((58, 44, 198, 184), fill=(250, 250, 248), outline=(130, 132, 134), width=3)
d.ellipse((104, 92, 152, 140), fill=(220, 224, 226), outline=(120, 124, 126), width=2)
leds(d, [(128, 116)], [(60, 200, 70)], 7)
centered(d, "ШС / СУХОЙ КОНТАКТ", 196, font(13), (60, 64, 66))
centered(d, "EOL 4,7 кОм", 218, font(11, False), (90, 94, 96))
save(img, "device_loop_sensor.png")

img, d = base((235, 236, 236), (248, 248, 246))
d.rounded_rectangle((60, 84, 196, 172), radius=6, fill=(250, 250, 248), outline=(130, 132, 134), width=2)
d.line((128, 84, 128, 172), fill=(180, 182, 184), width=2)
d.text((74, 112), "ДС", font=font(20), fill=(40, 44, 46))
d.text((150, 112), "N", font=font(20), fill=(40, 44, 46))
centered(d, "ДАТЧИК СОСТОЯНИЯ ДВЕРИ", 196, font(12), (60, 64, 66))
save(img, "device_door_contact.png")

img = Image.new("RGB", (16, 16), (90, 94, 98))
d = ImageDraw.Draw(img)
d.rectangle((1, 1, 14, 14), outline=(220, 220, 220))
d.text((3, 4), "B", font=font(8), fill=(240, 240, 240))
img.save(os.path.join(BLOCKS, "device_placeholder.png"), "PNG", optimize=True)

# 10. Six cable classes x 16 channels.
COLORS = [
    (240, 240, 240), (235, 135, 35), (215, 70, 190), (90, 190, 225),
    (235, 215, 45), (110, 205, 55), (235, 145, 175), (115, 115, 115),
    (175, 175, 175), (55, 195, 200), (145, 75, 205), (55, 95, 210),
    (125, 85, 55), (65, 165, 65), (220, 45, 40), (30, 30, 30),
]
KINDS = ["rs485", "dpl", "power12", "control24", "start24", "fire_zone"]
for k, kind in enumerate(KINDS):
    for i, col in enumerate(COLORS):
        img = Image.new("RGB", (16, 16), (38, 42, 44))
        d = ImageDraw.Draw(img)
        d.rectangle((0, 0, 15, 15), outline=(24, 26, 28))
        d.line((0, 8, 15, 8), fill=col, width=3)
        d.line((8, 0, 8, 15), fill=col, width=3)
        d.rectangle((6, 6, 9, 9), fill=(245, 245, 245), outline=(20, 20, 20))
        # small class marker in the corner
        marker = (col[0] // 2, col[1] // 2, col[2] // 2)
        for j in range(k + 1):
            d.point((1 + j * 2, 1), fill=marker)
        img.save(os.path.join(BLOCKS, "cable_%s_%d.png" % (kind, i)), "PNG", optimize=True)

# 11. Item icons.
def item(name, draw_fn):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    draw_fn(d)
    img.save(os.path.join(ITEMS, name + ".png"), "PNG", optimize=True)

def tool(d, color, letter):
    d.rounded_rectangle((2, 2, 13, 13), radius=2, fill=color, outline=(20, 24, 28))
    d.rectangle((4, 4, 11, 8), fill=(80, 170, 90), outline=(10, 30, 15))
    d.text((5, 8), letter, font=font(6), fill=(240, 240, 240))
item("programmer", lambda d: tool(d, (55, 70, 90), "P"))
item("multimeter", lambda d: tool(d, (190, 150, 45), "V"))
item("short_jumper", lambda d: (d.line((2, 8, 13, 8), fill=(220, 45, 40), width=3),
                                d.rectangle((2, 6, 4, 10), fill=(80, 80, 80)),
                                d.rectangle((11, 6, 13, 10), fill=(80, 80, 80))))
item("eol_resistor", lambda d: (d.line((1, 8, 4, 8), fill=(180, 180, 180)),
                                d.rectangle((4, 4, 11, 12), fill=(215, 195, 125), outline=(80, 65, 35)),
                                d.line((11, 8, 15, 8), fill=(180, 180, 180))))
item("terminator", lambda d: (d.rectangle((3, 6, 12, 10), fill=(60, 60, 65), outline=(20, 20, 20)),
                              d.line((1, 8, 3, 8), fill=(190, 190, 190)),
                              d.line((12, 8, 15, 8), fill=(190, 190, 190)),
                              d.text((5, 6), "120", font=font(5), fill=(240, 240, 240))))
item("reset_key", lambda d: (d.line((2, 12, 11, 3), fill=(210, 180, 70), width=2),
                             d.ellipse((10, 2, 14, 6), fill=(210, 180, 70), outline=(80, 60, 20))))
item("battery_7ah", lambda d: (d.rectangle((2, 4, 13, 14), fill=(45, 50, 55), outline=(15, 15, 15)),
                               d.rectangle((4, 2, 7, 4), fill=(190, 190, 190)),
                               d.rectangle((9, 2, 12, 4), fill=(190, 190, 190)),
                               d.text((4, 7), "7", font=font(7), fill=(240, 240, 240))))
item("battery_45ah", lambda d: (d.rectangle((1, 4, 14, 14), fill=(55, 45, 65), outline=(15, 15, 15)),
                                d.rectangle((3, 2, 6, 4), fill=(190, 190, 190)),
                                d.rectangle((9, 2, 12, 4), fill=(190, 190, 190)),
                                d.text((3, 7), "4.5", font=font(6), fill=(240, 240, 240))))
item("powder_cartridge", lambda d: (d.rounded_rectangle((5, 2, 10, 13), radius=2, fill=(190, 70, 45), outline=(70, 25, 20)),
                                    d.rectangle((6, 2, 9, 5), fill=(210, 180, 70)),
                                    d.line((6, 13, 4, 15), fill=(200, 40, 35)),
                                    d.line((9, 13, 11, 15), fill=(45, 60, 200))))

# 12. Optional GUI backdrop.
img = Image.new("RGB", (256, 256), (18, 24, 28))
d = ImageDraw.Draw(img)
for y in range(0, 256, 8):
    d.line((0, y, 255, y), fill=(22, 30, 34))
d.rectangle((0, 0, 255, 28), fill=(10, 16, 22))
centered(d, "BOLID / ORION  •  PProg / UProg", 6, font(13), (120, 220, 150))
d.rectangle((8, 36, 247, 247), outline=(60, 90, 80))
img.save(os.path.join(GUI, "device_bg.png"), "PNG", optimize=True)

print("Textures generated in", ROOT)
