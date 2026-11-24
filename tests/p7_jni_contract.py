"""Compare the compiled app's playback/activity methods to libmain's JNI ABI."""
from pathlib import Path
import re
import subprocess
import sys

build, symbol_file = map(Path, sys.argv[1:])
exports = {line.split()[-1] for line in symbol_file.read_text().splitlines() if line.strip()}
source = '\n'.join(p.read_text() for p in Path('src').glob('*.c'))
types = {'void':'V', 'jint':'I', 'jlong':'J', 'jboolean':'Z', 'jstring':'Ljava/lang/String;',
         'jshortArray':'[S', 'jintArray':'[I', 'jbyteArray':'[B', 'jfloat':'F', 'jdouble':'D'}
for owner in ('NvPlayer', 'NuvioActivity'):
    prefix = 'Java_space_nuvio_nativelegacy_' + owner + '_'
    binaries = list(build.glob('tmp/kotlin-classes/debug/space/nuvio/nativelegacy/' + owner + '.class'))
    if len(binaries) != 1:
        raise SystemExit(f'Expected one compiled {owner}.class, found {binaries}')
    bytecode = subprocess.check_output(['javap', '-p', '-s', str(binaries[0])], text=True)
    methods = re.findall(r'^  ([^\n(]+) (\w+)\([^\n]*\);\n\s+descriptor: (\S+)', bytecode, re.M)
    native = {(name, desc, 'static' in mods.split()) for mods, name, desc in methods if 'native' in mods.split()}
    c_methods = set()
    for ret, name, params in re.findall(r'JNIEXPORT\s+(\w+)\s+JNICALL\s+' + prefix + r'(\w+)\s*\(([^)]*)\)', source):
        args = [param.strip().split()[0] for param in params.split(',')]
        desc = '(' + ''.join(types[t] for t in args[2:]) + ')' + types[ret]
        c_methods.add((name, desc, args[1] == 'jclass'))
        assert prefix + name in exports, f'Missing ELF export {prefix}{name}'
    assert native and native <= c_methods, (owner, 'Missing or mismatched JNI signatures', native-c_methods)
    if c_methods-native:
        print(f'{owner}: additional C exports without Kotlin callers: {sorted(c_methods-native)}')
    print(f'{owner}: verified {len(native)} native callback signatures and exports')
    if owner == 'NvPlayer':
        lookups = re.findall(r'GetStaticMethodID\(env,\s*gCls,\s*"(\w+)",\s*"([^"]+)"\)', Path('src/video_android.c').read_text())
        available = {(name, desc) for mods, name, desc in methods if 'static' in mods.split()}
        assert lookups and not set(lookups)-available, set(lookups)-available
        print(f'{owner}: verified {len(lookups)} C-to-Kotlin static method lookups')
