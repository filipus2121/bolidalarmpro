#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate 1.7.10 .lang files (UTF-8, key=value)."""
import os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "bolidalarmpro", "lang")
os.makedirs(ROOT, exist_ok=True)

devices_ru = [
    "РИП-12 исп.02",
    "С2000М 2.05",
    "С2000М 5.02 исп.02",
    "С2000 КДЛ 2.36",
    "С2000 АСПТ 3.53",
    "ИПР-513 3АМ",
    "УДП 513-3АМ",
    "Пиропатрон порошковый",
    "Молния2-12 «Порошок! НЕ ВХОДИ!»",
    "Молния2-12 «Порошок! УХОДИ!»",
    "Стендовый ввод ~220 В",
    "Промежуточное реле 12 В",
    "Шлейфный извещатель (стенд)",
    "Датчик состояния двери (стенд)",
]
devices_en = [
    "RIP-12 isp.02", "S2000M 2.05", "S2000M 5.02 isp.02", "S2000-KDL 2.36",
    "S2000-ASPT 3.53", "IPR-513-3AM", "UDP 513-3AM", "Powder pyrotechnic cartridge",
    "Molniya2-12 'Powder! DO NOT ENTER!'", "Molniya2-12 'Powder! GET OUT!'",
    "Laboratory 220 V mains", "12 V intermediate relay", "Conventional loop sensor",
    "Door contact",
]
cables = {
    "rs485": ("Кабель RS-485 (витая пара)", "RS-485 twisted pair"),
    "dpl": ("Кабель ДПЛС 2х0,5", "DPL 2x0.5 cable"),
    "power12": ("Кабель питания +12 В", "+12 V power cable"),
    "control24": ("Кабель выхода 24 В / СО", "24 V controlled output cable"),
    "start24": ("Кабель цепи запуска АУП", "AUP start circuit cable"),
    "fire_zone": ("Кабель ШС / сухой контакт", "Fire loop / dry contact cable"),
}
colors_ru = ["белый", "оранжевый", "пурпурный", "голубой", "жёлтый", "салатовый",
             "розовый", "серый", "светло-серый", "бирюзовый", "фиолетовый", "синий",
             "коричневый", "зелёный", "красный", "чёрный"]
colors_en = ["white", "orange", "magenta", "light blue", "yellow", "lime",
             "pink", "gray", "light gray", "cyan", "purple", "blue",
             "brown", "green", "red", "black"]
items_ru = {
    "programmer": ("Программатор PProg / UProg", "ПКМ по прибору: конфигурация; ПКМ по кабелю: смена канала 0..15"),
    "multimeter": ("Мультиметр / тестер линии", "ПКМ по кабелю: U, I, R, EOL, терминаторы, качество связи, неисправность"),
    "short_jumper": ("КЗ-перемычка", "ПКМ по кабелю: внести/снять короткое замыкание для эксперимента"),
    "eol_resistor": ("Оконечный резистор 4,7 кОм", "ПКМ по кабелю: установить оконечный резистор ШС/выхода"),
    "terminator": ("Терминатор 120 Ом", "ПКМ по кабелю RS-485: установить согласующий резистор"),
    "reset_key": ("Ключ ИПР / УДП", "ПКМ по ручному извещателю: сброс в дежурный режим без замены стекла"),
    "battery_7ah": ("АКБ 12 В 7 А·ч", "Устанавливается в РИП-12 исп.02"),
    "battery_45ah": ("Комплект АКБ 2 x 4,5 А·ч", "Устанавливается в С2000-АСПТ"),
    "powder_cartridge": ("Порошковый пиропатрон", "Одноразовый инициатор учебного модуля порошкового тушения"),
}
items_en = {
    "programmer": ("PProg / UProg programmer", "Right-click a device to configure; right-click cable to change channel"),
    "multimeter": ("Line tester / multimeter", "Right-click cable for U, I, R, EOL, terminators and link quality"),
    "short_jumper": ("Short-circuit jumper", "Right-click cable to inject/remove a short"),
    "eol_resistor": ("4.7 kOhm end-of-line resistor", "Right-click supervised cable to install EOL"),
    "terminator": ("120 Ohm terminator", "Right-click RS-485 cable to install termination"),
    "reset_key": ("IPR / UDP reset key", "Right-click a manual call point to reset it"),
    "battery_7ah": ("12 V 7 Ah battery", "Install into RIP-12 isp.02"),
    "battery_45ah": ("2 x 4.5 Ah battery pack", "Install into S2000-ASPT"),
    "powder_cartridge": ("Powder pyrotechnic cartridge", "Single-use laboratory extinguishing initiator"),
}

def write(path, lines):
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

ru = ["itemGroup.bolidalarmpro=BOLID / Орион — стенд ОПС",
      "tile.bolidalarmpro.device.name=Прибор BOLID / Орион",
      "gui.bolid.reset.key=ИПР/УДП возвращён в дежурный режим ключом"]
en = ["itemGroup.bolidalarmpro=BOLID / Orion fire-alarm stand",
      "tile.bolidalarmpro.device.name=BOLID / Orion device",
      "gui.bolid.reset.key=Manual call point reset with key"]

for i, (a, b) in enumerate(zip(devices_ru, devices_en)):
    ru.append("tile.bolidalarmpro.device.%d.name=%s" % (i, a))
    en.append("tile.bolidalarmpro.device.%d.name=%s" % (i, b))
for kind, (ru_name, en_name) in cables.items():
    ru.append("tile.bolidalarmpro.cable.%s.name=%s" % (kind, ru_name))
    en.append("tile.bolidalarmpro.cable.%s.name=%s" % (kind, en_name))
    for i in range(16):
        ru.append("tile.bolidalarmpro.cable.%s.%d.name=%s / канал %d (%s)" % (kind, i, ru_name, i, colors_ru[i]))
        en.append("tile.bolidalarmpro.cable.%s.%d.name=%s / channel %d (%s)" % (kind, i, en_name, i, colors_en[i]))
for key, (name, desc) in items_ru.items():
    ru.append("item.bolidalarmpro.%s.name=%s" % (key, name))
    ru.append("item.bolidalarmpro.%s.desc=%s" % (key, desc))
for key, (name, desc) in items_en.items():
    en.append("item.bolidalarmpro.%s.name=%s" % (key, name))
    en.append("item.bolidalarmpro.%s.desc=%s" % (key, desc))

write(os.path.join(ROOT, "ru_RU.lang"), ru)
write(os.path.join(ROOT, "en_US.lang"), en)
print("Lang files written:", len(ru), "ru /", len(en), "en lines")
