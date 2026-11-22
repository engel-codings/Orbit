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
sleep 9
shot 01_bonus
tap 360 895; sleep 2.5;  shot 02_menu
tap 360 688; sleep 2;    shot 03_karte
tap 501 1276; sleep 2;   shot 04_level1
tap 68 83; sleep 1.2;    shot 05_pause
tap 360 722; sleep 0.8
tap 360 1000; sleep 0.35; shot 06_flug
sleep 2.5;               shot 07_ende
back; sleep 1.5
back; sleep 1.5
tap 112 1434; sleep 1.5; shot 08_shop_skins
adb shell input swipe 360 1300 360 500 400; sleep 1; shot 09_shop_skins2
tap 360 188; sleep 1.5;  shot 10_shop_spuren
tap 576 188; sleep 1.5;  shot 11_shop_extras
back; sleep 1.5
tap 277 1434; sleep 1.5; shot 12_erfolge
back; sleep 1.5
tap 608 1434; sleep 1.5; shot 13_optionen
back; sleep 1.5
tap 443 1434; sleep 1.5; shot 14_statistik
back; sleep 1.5
tap 486 1060; sleep 1.5; shot 15_gluecksrad
tap 360 1025; sleep 1.2; shot 16_dreht
sleep 3.5;               shot 17_gewinn
back; sleep 1.5
tap 234 1060; sleep 1.5; shot 18_challenge
tap 360 1000; sleep 0.4; shot 19_challenge_flug
# App neu starten (sauberer Zustand im Menü)
adb shell am force-stop de.engelcode.orbit
adb shell am start -n de.engelcode.orbit/.MainActivity
sleep 6
# Sprachen testen: Optionen -> Sprache wechseln
tap 608 1434; sleep 1.5
tap 360 737; sleep 1.2;  shot 20_optionen_EN
back; sleep 1.5;         shot 21_menu_EN
tap 112 1434; sleep 1.5; shot 22_shop_EN
back; sleep 1.5
tap 608 1434; sleep 1.5
tap 360 737; sleep 1.2;  shot 23_optionen_RU
back; sleep 1.5;         shot 24_menu_RU
tap 112 1434; sleep 1.5; shot 25_shop_RU
back; sleep 1.5
tap 277 1434; sleep 1.5; shot 26_erfolge_RU
back; sleep 1.5
tap 443 1434; sleep 1.5; shot 27_statistik_RU
back; sleep 1.5
tap 360 688; sleep 2;    shot 28_karte_RU
tap 501 1276; sleep 2;   shot 29_level_RU
adb logcat -d -s AndroidRuntime:E > shots/crashlog.txt || true
ls -la shots
