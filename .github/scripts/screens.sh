#!/bin/bash
# Startet Orbit im Emulator (720x1600 wie Redmi 9AT) und macht Screenshots.
set -x
mkdir -p shots
shot() { sleep 0.3; adb exec-out screencap -p > "shots/$1.png"; }
tap() { adb shell input tap "$1" "$2"; }
back() { adb shell input keyevent 4; }

adb shell settings put secure immersive_mode_confirmations confirmed
adb install -r Orbit.apk
adb shell am start -n de.engelcode.orbit/.MainActivity
sleep 8
shot 01_bonus
tap 360 891; sleep 2;   shot 02_menu
tap 360 662; sleep 2;   shot 03_karte
tap 501 1276; sleep 2;  shot 04_level1
tap 360 1000; sleep 0.35; shot 05_flug
sleep 2;                shot 06_spiel
back; sleep 1;          shot 07_pause
tap 360 947; sleep 1.5
back; sleep 1.5
tap 248 885; sleep 1.5; shot 08_shop_skins
tap 360 186; sleep 1.5; shot 09_shop_spuren
tap 585 186; sleep 1.5; shot 10_shop_extras
back; sleep 1.5
tap 360 989; sleep 1.5; shot 11_einstellungen
back; sleep 1.5
tap 472 885; sleep 1.5; shot 12_statistik
back; sleep 1.5
tap 360 778; sleep 1.5
tap 360 1000; sleep 1.5; shot 13_endlos
adb logcat -d -s AndroidRuntime:E > shots/crashlog.txt || true
ls -la shots
