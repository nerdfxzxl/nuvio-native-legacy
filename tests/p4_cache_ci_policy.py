#!/usr/bin/env python3
"""P4 cache policy tests compiled from exact checked-out Kotlin production sources.

Runs on a CI host after Gradle has resolved the project's Kotlin 2.0.21 plugin.
No APK, Android emulator, network service, or production data is used.
"""
from pathlib import Path
import os
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
prod = root / "android/app/src/main/java/space/nuvio/nativelegacy"
cache = Path.home() / ".gradle/caches/modules-2/files-2.1"


def first(pattern):
    candidates = sorted(cache.glob(pattern))
    if not candidates:
        raise RuntimeError("Gradle dependency not cached: " + pattern)
    return candidates[-1]


stdlib = first("org.jetbrains.kotlin/kotlin-stdlib/2.0.21/*/*.jar")
compiler = [first("org.jetbrains.kotlin/kotlin-compiler-embeddable/2.0.21/*/*.jar")]
for pattern in (
    "org.jetbrains.kotlin/kotlin-script-runtime/2.0.21/*/*.jar",
    "org.jetbrains.intellij.deps/trove4j/*/*/*.jar",
    "org.jetbrains/annotations/13.0/*/*.jar",
    "org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/*/*/*.jar",
):
    compiler.append(first(pattern))

java = os.environ.get("NUVIO_TEST_JAVA", "java")
compiler_cp = os.pathsep.join(str(jar) for jar in [*compiler, stdlib])
cases = [
    ("provenance and strong HTTP validators",
     ["CacheProvaP4C.kt"], "CacheProvaP4CTest.kt", "CacheProvaP4CTestKt"),
    ("real disk span corruption and eviction",
     ["CacheSpanP4C.kt"], "CacheSpanP4CTest.kt", "CacheSpanP4CTestKt"),
    ("session identity and storage limits",
     ["CacheSessao.kt"], "CacheSessaoP4CTest.kt", "CacheSessaoP4CTestKt"),
]
with tempfile.TemporaryDirectory(prefix="nuvio-p4-policy-") as tmp:
    for index, (name, production, test, main) in enumerate(cases):
        out = Path(tmp) / ("case" + str(index))
        out.mkdir()
        sources = [prod / name for name in production] + [root / "tests" / test]
        subprocess.run([
            java, "-cp", compiler_cp, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            "-no-reflect", "-no-stdlib", "-jvm-target", "17",
            "-classpath", str(stdlib), "-d", str(out),
            *map(str, sources),
        ], check=True)
        subprocess.run([
            java, "-cp", os.pathsep.join((str(out), str(stdlib))), main,
        ], check=True)
        print("PASS: checked-out production Kotlin:", name, flush=True)
print("P4 production Kotlin policy tests passed; NOT an Android APK or device test.")
