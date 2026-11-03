#!/usr/bin/env python3
"""把 base/ 资源与 classes.dex 重新打包成 APK（不压缩，交给 zipalign 对齐）。"""
import os, sys, zipfile

def main():
    base, dex, out = sys.argv[1], sys.argv[2], sys.argv[3]
    with zipfile.ZipFile(out, "w", zipfile.ZIP_STORED) as z:
        for root, dirs, files in os.walk(base):
            dirs.sort()
            for f in sorted(files):
                p = os.path.join(root, f)
                rel = os.path.relpath(p, base)
                z.write(p, rel)
        z.write(dex, "classes.dex")
    print("repacked ->", out)

if __name__ == "__main__":
    main()
