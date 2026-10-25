# Baaja Dev Guide

This file outlines the process of setting up a local dev environment for Baaja.

## Prerequisites

- JDK 21
- Android platform tools (if you don't have a keystore already)

## Basic setup

This has been tested on Linux, but should work on other platforms with some adjustments.

```bash
git clone https://github.com/harshaltiwari-build/Baaja
cd Baaja
git submodule update --init --recursive
cd ..
[ ! -f "app/persistent-debug.keystore" ] && keytool -genkeypair -v -keystore app/persistent-debug.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" || echo "Keystore already exists."
./gradlew :app:assembleFossDebug
ls app/build/outputs/apk/foss/debug/app-foss-debug.apk
```

## AI note

Baaja does allow LLM generated contributions, but you should follow guidelines in AGENTS.md to ensure the quality of the contributions. Please use the ponytail skill from this url - https://github.com/DietrichGebert/ponytail/raw/refs/heads/main/skills/ponytail/SKILL.md - this is needed to keep work minimal and clean.  
Do not try to hide your LLM contributions, we know what you are doing.