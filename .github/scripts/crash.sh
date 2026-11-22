#!/bin/bash
# Sucht Abstürze: spielt automatisch und sammelt das Absturzprotokoll.
set -x
mkdir -p crash
adb shell settings put secure immersive_mode_confirmations confirmed
adb install -r Orbit.apk
adb logcat -c
adb shell am start -n de.engelcode.orbit/.MainActivity
sleep 9
adb shell input tap 360 895; sleep 2
# Endlos Dimension 1 spielen (viele Runden)
adb shell input tap 360 965; sleep 1.5
adb shell input tap 190 270; sleep 1
for i in $(seq 1 300); do
  adb shell input tap 360 900
  sleep 0.35
  if [ $((i % 20)) -eq 0 ]; then adb shell input tap 360 1123; adb exec-out screencap -p > crash/play_$i.png; fi
done
adb logcat -d -b crash > crash/crash1.txt
# Level-Karte spielen
adb shell am force-stop de.engelcode.orbit
adb shell am start -n de.engelcode.orbit/.MainActivity
sleep 6
adb shell input tap 360 688; sleep 2
adb shell input tap 501 1276; sleep 2
for i in $(seq 1 200); do
  adb shell input tap 360 900
  sleep 0.45
  if [ $((i % 25)) -eq 0 ]; then adb shell input tap 360 1123; adb shell input tap 360 1000; adb exec-out screencap -p > crash/lv_$i.png; fi
done
adb logcat -d -b crash > crash/crash2.txt
# Zufallstest
adb shell monkey -p de.engelcode.orbit --throttle 120 --pct-syskeys 0 --pct-appswitch 0 -v 4000 > crash/monkey.txt 2>&1
adb logcat -d -b crash > crash/crash3.txt
adb logcat -d | grep -iE "FATAL|AndroidRuntime|Exception|orbit" | tail -200 > crash/logcat.txt
cat crash/crash*.txt
