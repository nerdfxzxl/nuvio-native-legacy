from pathlib import Path
p = Path("android/app/src/main/java/space/nuvio/nativelegacy/NvPlayer.kt").read_text()
o = Path("android/app/src/main/java/space/nuvio/nativelegacy/AudioRouteObserverP5D.kt").read_text()
c = Path("src/video_android.c").read_text()
route = p.split("private fun startAudioRoute(", 1)[1].split("// --- escolha de faixa", 1)[0]
assert "getDevices(" not in o and ".routedDevice" in o
assert "registerAudioDeviceCallback" in o and "removeOnRoutingChangedListener" in o
assert ".setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, disabled)" in p
assert "p.createMessage" in route and "r.routeTx.owns(ticket)" in route
assert "p.stop()" not in route and "p.seekTo(" not in route and "setMediaItem(" not in route
assert "p.playWhenReady =" not in route
assert "routeBlocked(r)" in route and "r.routeTx.expired(now)" in route
assert "nativeAudioRouteCapacidade" in p and "nativeAudioRouteCapacidade" in c
assert "EV_AUDIO_SAIDA_INCOMPATIVEL" in p and "audioRouteDeadline" in c
assert "routeObserver.stop()" in p and "routeForeground = false" in p
print("P5-D source integration contracts PASS (not device execution)")
