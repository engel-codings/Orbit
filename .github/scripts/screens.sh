#!/bin/bash
# Startet Orbit im Emulator (720x1600 wie Redmi 9AT) und macht Screenshots.
set -x
mkdir -p shots
shot() { sleep 0.3; adb exec-out screencap -p > "shots/$1.png"; }
tap() { adb shell input tap "$1" "$2"; }
back() { adb shell input keyevent 4; }
NAVY=1431; ROWY=1060

adb shell settings put secure immersive_mode_confirmations confirmed
adb install -r Orbit.apk
adb shell am start -n de.engelcode.orbit/.MainActivity
sleep 9
shot 01_bonus
tap 360 895; sleep 2.5;   shot 02_menu
tap 360 688; sleep 2;     shot 03_karte
tap 501 1276; sleep 2;    shot 04_level1
tap 68 83; sleep 1.2;     shot 05_pause
tap 360 722; sleep 0.8
tap 360 1000; sleep 0.35; shot 06_flug
sleep 2.5;                shot 07_ende
adb shell am force-stop de.engelcode.orbit
adb shell am start -n de.engelcode.orbit/.MainActivity
sleep 6
tap 72 $NAVY; sleep 1.5;  shot 08_shop_skins
adb shell input swipe 360 1300 360 300 400; sleep 1; shot 09_shop_skins2
adb shell input swipe 360 1300 360 300 400; sleep 1; shot 10_shop_skins3
tap 360 188; sleep 1.5;   shot 11_shop_spuren
tap 576 188; sleep 1.5;   shot 12_shop_extras
back; sleep 1.5
tap 216 $NAVY; sleep 1.5; shot 13_missionen
back; sleep 1.5
tap 360 $NAVY; sleep 1.5; shot 14_erfolge
back; sleep 1.5
tap 504 $NAVY; sleep 1.5; shot 15_statistik
back; sleep 1.5
tap 648 $NAVY; sleep 1.5; shot 16_optionen
back; sleep 1.5
tap 585 $ROWY; sleep 1.5; shot 17_truhe
tap 360 $ROWY; sleep 1.5; shot 18_gluecksrad
tap 360 1025; sleep 4.8;  shot 19_gewinn
back; sleep 1.5
tap 216 $NAVY; sleep 1.5; shot 20_missionen2
back; sleep 1.5
tap 134 $ROWY; sleep 1.5; shot 21_challenge
adb shell am force-stop de.engelcode.orbit
adb shell am start -n de.engelcode.orbit/.MainActivity
sleep 6
tap 360 965; sleep 1.5;   shot 21b_endlos_auswahl
tap 360 270; sleep 1.5;   shot 21c_endlos
# Sprachen: App neu starten
adb shell am force-stop de.engelcode.orbit
adb shell am start -n de.engelcode.orbit/.MainActivity
sleep 6
tap 648 $NAVY; sleep 1.5
tap 360 688; sleep 1.2;   shot 22_optionen_RU
back; sleep 1.5;          shot 23_menu_RU
tap 216 $NAVY; sleep 1.5; shot 24_missionen_RU
back; sleep 1.5
tap 648 $NAVY; sleep 1.5
tap 360 688; sleep 1.2;   shot 25_optionen_DE
back; sleep 1.5;          shot 26_menu_DE
tap 216 $NAVY; sleep 1.5; shot 27_missionen_DE
adb logcat -d -s AndroidRuntime:E > shots/crashlog.txt || true
ls -la shots
