#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Lightweight source validation: Java syntax + simulator purity."""
import glob
import os
import sys

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
java_files = sorted(glob.glob(os.path.join(root, "src", "**", "*.java"), recursive=True))
errors = []

try:
    import javalang
except ImportError:
    javalang = None

for path in java_files:
    text = open(path, encoding="utf-8").read()
    if javalang is not None:
        try:
            javalang.parse.parse(text)
        except Exception as exc:
            errors.append((path, str(exc)))
    else:
        # Minimal fallback if javalang is not installed.
        for open_ch, close_ch in (("{", "}"), ("(", ")"), ("[", "]")):
            if text.count(open_ch) != text.count(close_ch):
                errors.append((path, "unbalanced " + open_ch + close_ch))

for path in glob.glob(os.path.join(root, "src", "main", "java", "ru", "bolidalarmpro", "sim", "*.java")):
    text = open(path, encoding="utf-8").read()
    for forbidden in ("net.minecraft", "cpw.mods", "net.minecraftforge", "org.lwjgl"):
        if forbidden in text:
            errors.append((path, "sim package must not depend on " + forbidden))

print("Checked %d Java files; %d errors" % (len(java_files), len(errors)))
for path, msg in errors[:20]:
    print(os.path.relpath(path, root) + ": " + msg)
sys.exit(1 if errors else 0)
