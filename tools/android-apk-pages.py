#!/usr/bin/env python3
"""Check 16KB ELF load/RELRO alignment of every ARM64/x86_64 library in an APK.

Uses only Python's standard library, including for compressed native libraries.
32-bit libraries are excluded from the Android 16KB compatibility requirement.
"""
import argparse
from pathlib import Path
import struct
import sys
import zipfile

PAGE_SIZE = 16384
MACHINES = {'arm64-v8a': 183, 'x86_64': 62}
PT_LOAD = 1
PT_GNU_RELRO = 0x6474E552
PF_W = 2


def check_elf(data, abi):
    """Return load-segment count, minimum alignment and a list of errors."""
    if len(data) < 64 or data[:4] != b'\x7fELF':
        raise ValueError('missing/truncated ELF64 header')
    if data[4:7] != bytes((2, 1, 1)):
        raise ValueError('expected a little-endian ELF64 file')
    header = struct.unpack_from('<16sHHIQQQIHHHHHH', data)
    if header[1] != 3 or header[2] != MACHINES[abi]:
        raise ValueError(f'ELF type/machine does not match {abi}')
    phoff, entsize, count = header[5], header[9], header[10]
    if header[8] < 64 or entsize < 56 or count == 0 or count == 0xFFFF:
        raise ValueError('invalid/unsupported ELF program-header table')
    if phoff < 64 or phoff + entsize * count > len(data):
        raise ValueError('truncated ELF program-header table')

    loads, relros, errors = [], [], []
    for index in range(count):
        kind, flags, offset, address, _, file_size, memory_size, align = struct.unpack_from(
            '<IIQQQQQQ', data, phoff + index * entsize)
        if kind == PT_LOAD:
            loads.append({
                'index': index,
                'flags': flags,
                'start': address,
                'end': address + memory_size,
                'align': align,
            })
            if align < PAGE_SIZE or align & (align - 1):
                errors.append(
                    f'LOAD[{index}] alignment={align}, expected a power of two >= {PAGE_SIZE}'
                )
            elif offset % align != address % align:
                errors.append(
                    f'LOAD[{index}] offset/address are not congruent modulo alignment'
                )
            if file_size > memory_size or offset + file_size > len(data):
                errors.append(f'LOAD[{index}] invalid file/memory range')
        elif kind == PT_GNU_RELRO:
            relros.append((index, address, address + memory_size))

    if not loads:
        raise ValueError('ELF has no LOAD segment')

    # Android's 16 KB rule allows a non-aligned RELRO end when no writable
    # data occupies the remainder of the 16 KB page that the linker protects.
    # This includes the documented case where RELRO is the tail of its LOAD
    # segment and the next writable LOAD starts at the next 16 KB boundary.
    for index, relro_start, relro_end in relros:
        if relro_end % PAGE_SIZE == 0:
            continue

        protected_page_end = (
            (relro_end + PAGE_SIZE - 1) // PAGE_SIZE
        ) * PAGE_SIZE
        following_writable = [
            load for load in loads
            if (load['flags'] & PF_W)
            and load['start'] < protected_page_end
            and load['end'] > relro_end
        ]
        if following_writable:
            details = ', '.join(
                f"LOAD[{load['index']}] range=0x{load['start']:x}-0x{load['end']:x}"
                for load in following_writable
            )
            errors.append(
                f'GNU_RELRO[{index}] end=0x{relro_end:x} is not {PAGE_SIZE}-byte aligned; '
                f'writable data occupies the protected trailing page ({details})'
            )

    return len(loads), min(load['align'] for load in loads), errors


def _synthetic_elf(program_headers, abi='arm64-v8a'):
    """Build a minimal ELF header for deterministic checker self-tests."""
    phoff, phentsize = 64, 56
    ident = b'\x7fELF' + bytes((2, 1, 1)) + bytes(9)
    header = struct.pack(
        '<16sHHIQQQIHHHHHH',
        ident, 3, MACHINES[abi], 1, 0, phoff, 0, 0,
        64, phentsize, len(program_headers), 0, 0, 0
    )
    return header + b''.join(
        struct.pack('<IIQQQQQQ', *entry) for entry in program_headers
    )


def self_test():
    """Exercise aligned, permitted-exception, and genuinely unsafe RELRO cases."""
    load = lambda flags, memsz, align=PAGE_SIZE: (
        PT_LOAD, flags, 0, 0x4000, 0, 0, memsz, align
    )
    relro = lambda memsz: (
        PT_GNU_RELRO, 4, 0, 0x4000, 0, 0, memsz, 1
    )

    cases = [
        ('aligned RELRO', [load(6, PAGE_SIZE), relro(PAGE_SIZE)], False),
        ('RELRO ending with writable LOAD', [load(6, 0x1000), relro(0x1000)], False),
        ('next writable LOAD starts at page boundary', [
            load(4, 0x1000),
            relro(0x1000),
            (PT_LOAD, 6, 0, 0x8000, 0, 0, 0x1000, PAGE_SIZE),
        ], False),
        ('writable data after RELRO in same LOAD', [load(6, 0x2000), relro(0x1000)], True),
        ('separate writable LOAD starts inside protected page', [
            load(4, 0x1000),
            relro(0x1000),
            (PT_LOAD, 6, 0, 0x6000, 0, 0, 0x1000, PAGE_SIZE),
        ], True),
        ('no RELRO segment', [load(6, 0x1000)], False),
        ('under-aligned LOAD', [load(6, 0x1000, 4096)], True),
    ]
    for name, headers, should_fail in cases:
        _, _, errors = check_elf(_synthetic_elf(headers), 'arm64-v8a')
        if bool(errors) != should_fail:
            raise AssertionError(
                f'checker self-test failed for {name}: expected failure={should_fail}, '
                f'got errors={errors}'
            )


def check_apk(path, output=sys.stdout):
    checked = failures = skipped = 0
    seen = set()
    with zipfile.ZipFile(path) as apk:
        for item in sorted(apk.infolist(), key=lambda entry: entry.filename):
            parts = item.filename.split('/')
            if len(parts) != 3 or parts[0] != 'lib' or not parts[2].endswith('.so'):
                continue
            if parts[1] not in MACHINES:
                skipped += 1
                continue
            checked += 1
            try:
                if item.filename in seen:
                    raise ValueError('duplicate library path')
                seen.add(item.filename)
                count, minimum, errors = check_elf(apk.read(item), parts[1])
                if errors:
                    failures += 1
                    print(f'UNALIGNED {item.filename}: {"; ".join(errors)}', file=output)
                else:
                    print(
                        f'ALIGNED {item.filename}: {count} LOAD segments, '
                        f'min alignment={minimum}',
                        file=output
                    )
            except (ValueError, struct.error, zipfile.BadZipFile) as error:
                failures += 1
                print(f'INVALID {item.filename}: {error}', file=output)
    print(
        f'android-apk-pages: {checked} 64-bit libraries, {failures} failed, '
        f'{skipped} other-ABI libraries excluded',
        file=output
    )
    return failures == 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', nargs='?', type=Path)
    parser.add_argument(
        '--self-test', action='store_true',
        help='run synthetic ELF alignment tests without an APK'
    )
    args = parser.parse_args()

    try:
        self_test()
        print('android-apk-pages: RELRO checker self-tests passed')
        if args.self_test:
            return 0
        if args.apk is None:
            parser.error('an APK path is required unless --self-test is used')
        return 0 if check_apk(args.apk) else 1
    except (OSError, zipfile.BadZipFile) as error:
        print(f'android-apk-pages: {error}', file=sys.stderr)
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
