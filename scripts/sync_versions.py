"""Copies the master Minecraft code (version-26.2/src/{main,dev}/java) to version-26.1.2 and version-26.3, and the loader sources
(forge-26.1.2, neoforge-26.1.2) to the other loader layers.

Edit the code in version-26.2; never edit the copies. The few API differences are applied here:
  - 26.3 removed the block codec() method (the block between the CODEC-BEGIN and CODEC-END markers) and moved dropping an item at a player to Containers.
  - 26.1.2 has the older Minecraft accessors the dev screenshot tour uses (screen, overlay, toasts, render target).

Usage: python scripts/sync_versions.py
"""
import os
import re
import shutil
import time

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MASTER = "26.2"


def copy_tree(src, dst, transform=None, variants=None):
    written = set()
    for base, _, files in os.walk(src):
        for name in files:
            if not name.endswith(".java"):
                continue
            path = os.path.join(base, name)
            rel = os.path.relpath(path, src)
            out = os.path.join(dst, rel)
            written.add(os.path.abspath(out))
            os.makedirs(os.path.dirname(out), exist_ok=True)
            text = open(path, encoding="utf-8").read()
            if variants and name in variants:
                text = open(variants[name], encoding="utf-8").read()
            elif transform:
                text = transform(text)
            with open(out, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)
    # A file removed from the source is removed from the copy too.
    for base, _, files in os.walk(dst):
        for name in files:
            path = os.path.abspath(os.path.join(base, name))
            if name.endswith(".java") and path not in written:
                os.remove(path)


def v263(text):
    # 26.3 renamed the push reaction BLOCK to IMMOVEABLE.
    text = text.replace("PushReaction.BLOCK", "PushReaction.IMMOVEABLE")
    if "// CODEC-BEGIN" not in text:
        return text
    text = re.sub(r"\n    // CODEC-BEGIN\n.*?\n    // CODEC-END\n", "\n", text, flags=re.S)
    text = re.sub(r"\n    private static final MapCodec<[^\n]*\n", "\n", text)
    text = text.replace("import com.mojang.serialization.MapCodec;\n", "")
    return text


def loader263(text):
    # NeoForge 26.3 renamed the COMMON config type to LOCAL (and SERVER to SYNCED); the file name stays the same.
    return text.replace("ModConfig.Type.COMMON", "ModConfig.Type.LOCAL")


def v2612(text):
    text = text.replace("Blocks.BED.white()", "Blocks.WHITE_BED")
    text = text.replace("        villager.setVillagerDataFinalized(true);" + chr(10), "")
    return text.replace("net.minecraft.world.entity.EntityTypes.VILLAGER", "net.minecraft.world.entity.EntityType.VILLAGER")


def main():
    for v, transform in (("26.1.2", v2612), ("26.3", v263)):
        for kind in ("main", "dev"):
            src = os.path.join(ROOT, f"version-{MASTER}", "src", kind, "java")
            dst = os.path.join(ROOT, f"version-{v}", "src", kind, "java")
            if not os.path.isdir(src):
                continue
            if v == "26.1.2":
                copy_tree(src, dst, transform)
            else:
                copy_tree(src, dst, transform)
    for loader in ("neoforge", "forge"):
        src = os.path.join(ROOT, f"{loader}-26.1.2", "src")
        for v in ("26.2", "26.3"):
            for kind in ("main", "dev"):
                s = os.path.join(src, kind, "java")
                if os.path.isdir(s):
                    d = os.path.join(ROOT, f"{loader}-{v}", "src", kind, "java")
                    copy_tree(s, d, loader263 if (loader == "neoforge" and v == "26.3") else None)
    print("synced")


if __name__ == "__main__":
    main()
