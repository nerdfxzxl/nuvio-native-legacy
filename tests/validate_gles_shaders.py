#!/usr/bin/env python3
"""Validate the GLES fragment programs assembled by src/gfx.c.

C compilation cannot detect missing GLSL functions. This reproduces the
FS_CABECA + optional FS_SDF/FS_COVER + FS_CORPO composition used by
progMontar() and compiles every inline GLSL fragment with glslangValidator.

Shader bodies supplied by external C macros are not expanded by this test.
"""
import argparse
import ast
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
C_PATH = ROOT / "src/gfx.c"
H_PATH = ROOT / "src/gfx.h"
LITERALS = re.compile(r'"(?:\\.|[^"\\])*"', re.S)


def without_c_comments(source):
    pattern = re.compile(
        r'"(?:\\.|[^"\\])*"|'
        r"'(?:\\.|[^'\\])*'|"
        r"//[^\n]*|/\*.*?\*/",
        re.S,
    )
    return pattern.sub(
        lambda m: m.group(0) if m.group(0)[0] in ("'", '"') else " ",
        source,
    )


def initializer(source, declaration):
    start = source.find(declaration)
    if start < 0:
        raise ValueError(f"Missing shader declaration: {declaration}")
    start = source.index("=", start) + 1
    quoted = escaped = False
    for end in range(start, len(source)):
        char = source[end]
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
        elif char == '"':
            quoted = True
        elif char == ";":
            return source[start:end].strip()
    raise ValueError(f"Unterminated shader declaration: {declaration}")


def strings(expression):
    return "".join(ast.literal_eval(m.group()) for m in LITERALS.finditer(expression))


def split_top_level(expression):
    entries, previous, depth, quoted, escaped = [], 0, 0, False, False
    for index, char in enumerate(expression):
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
            continue
        if char == '"':
            quoted = True
        elif char in "([{":
            depth += 1
        elif char in ")]}":
            depth -= 1
        elif char == "," and depth == 0:
            entries.append(expression[previous:index].strip())
            previous = index + 1
    if expression[previous:].strip():
        entries.append(expression[previous:].strip())
    return entries


def shader_modes(header):
    match = re.search(r"typedef\s+enum\s*\{(.*?)\}\s*GfxModo\s*;", header, re.S)
    if not match:
        raise ValueError("Could not find GfxModo enum")
    values = {name: int(value) for name, value in
              re.findall(r"\b(GFX_[A-Z0-9_]+)\s*=\s*(\d+)\b", match.group(1))}
    if len(values) < 40 or "GFX_TEXTO" not in values or "GFX_NMODOS" not in values:
        raise ValueError("Incomplete GfxModo enum mapping")
    return values


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--static-only", action="store_true",
                        help="check dependencies without running glslangValidator")
    args = parser.parse_args()
    source = without_c_comments(C_PATH.read_text(encoding="utf-8"))
    header = without_c_comments(H_PATH.read_text(encoding="utf-8"))
    modes = shader_modes(header)
    count = modes["GFX_NMODOS"]

    config = initializer(source, "PRECISA[GFX_NMODOS]")
    flags = [(int(a), int(b)) for a, b in
             re.findall(r"\{\s*([01])\s*,\s*([01])\s*\}", config)]
    if len(flags) != count:
        raise ValueError(f"PRECISA has {len(flags)} entries; expected {count}")

    body_table = initializer(source, "FS_CORPO[GFX_NMODOS]")
    if not body_table.startswith("{") or not body_table.endswith("}"):
        raise ValueError("Expected FS_CORPO array initializer")
    bodies = {}
    implicit = 0
    for entry in split_top_level(body_table[1:-1]):
        explicit = re.match(r"\[\s*(GFX_[A-Z0-9_]+)\s*\]\s*=\s*", entry)
        if explicit:
            key = explicit.group(1)
            if key not in modes:
                raise ValueError(f"Unknown shader mode: {key}")
            implicit = modes[key]
            entry = entry[explicit.end():]
        if implicit >= count:
            raise ValueError(f"Shader initializer beyond GFX_NMODOS: {implicit}")
        if implicit in bodies:
            raise ValueError(f"Duplicate shader mode {implicit}")
        bodies[implicit] = strings(entry)
        implicit += 1

    if not bodies.get(modes["GFX_TEXTO"]):
        raise ValueError("GFX_TEXTO shader body missing: cannot protect regression")
    head = "precision mediump float;\n" + strings(initializer(source, "FS_CABECA"))
    sdf = strings(initializer(source, "FS_SDF"))
    cover = strings(initializer(source, "FS_COVER"))
    names = {v: k for k, v in modes.items()}
    failures = []
    fragments = {}
    skipped = []

    for mode in range(count):
        body = bodies.get(modes["GFX_CARD"] if mode == modes["GFX_CARD_HERO"] else mode, "")
        if not body:
            skipped.append(names.get(mode, str(mode)))
            continue
        # A missing helper was exactly what caused the build #98 black screen.
        calls_sdf = bool(re.search(r"\b(?:sdf|borda|bordaR)\s*\(", body))
        calls_cover = bool(re.search(r"\b(?:cover|coverAsp)\s*\(", body))
        if calls_sdf and not flags[mode][0]:
            failures.append(f"{names[mode]}: uses sdf/borda but PRECISA.sdf=0")
        if calls_cover and not flags[mode][1]:
            failures.append(f"{names[mode]}: uses cover but PRECISA.cover=0")
        fragments[mode] = (
            "#version 100\n" + head
            + ("#define HERO_CARD\n" if mode == modes["GFX_CARD_HERO"] else "")
            + (sdf if flags[mode][0] else "")
            + (cover if flags[mode][1] else "")
            + body
        )
    if failures:
        for failure in failures:
            print(f"::error::{failure}", file=sys.stderr)
        return 1
    if args.static_only:
        print(f"Shader helper checks passed: {len(fragments)} modes "
              f"({len(skipped)} external-macro/empty modes skipped)")
        return 0
    compiler = shutil.which("glslangValidator")
    if not compiler:
        print("::error::glslangValidator is required (install glslang-tools)",
              file=sys.stderr)
        return 1

    with tempfile.TemporaryDirectory(prefix="nuvio-shaders-") as folder:
        for mode, fragment in fragments.items():
            name = names.get(mode, str(mode))
            path = Path(folder) / f"{mode:02d}-{name}.frag"
            path.write_text(fragment, encoding="utf-8")
            result = subprocess.run(
                [compiler, "-S", "frag", str(path)],
                capture_output=True, text=True, check=False,
            )
            if result.returncode:
                failures.append(f"{name}:\n{result.stdout}{result.stderr}")
    if failures:
        for failure in failures:
            print(f"::error::{failure}", file=sys.stderr)
        return 1

    print(f"GLES fragment shader validation passed: {len(fragments)} compiled, "
          f"{len(skipped)} external-macro/empty modes skipped")
    return 0


if __name__ == "__main__":
    sys.exit(main())
