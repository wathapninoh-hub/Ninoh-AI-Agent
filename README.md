# NINOH AI AGENT

An Android-first, offline AI chat and coding app. The app runs local GGUF models through llama.cpp; no API key or internet connection is needed after the app and a model have been installed.

## Features
- Offline chat and coding mode
- Custom system rules saved on the device
- Import a local `.gguf` model using Android's file picker
- Model files remain on-device
- GitHub Actions builds a debug APK

## Requirements
- Android 8.0+ (API 26)
- 64-bit ARM Android device (arm64-v8a)
- A GGUF instruction model using a ChatML-style conversation format. For the intended small-model setup, choose a quantized model whose file is below 800 MB. Actual speed and memory use depend on the device and model.

## Build
Open this repository in Android Studio, or use the **Android APK** workflow under Actions. The workflow builds a debug APK and publishes it as an artifact.

## First launch
1. Install the APK.
2. Open **Model** and import a GGUF file already on your phone.
3. Select **Chat** or **Coding**, write a message, and tap **Send**.
4. Add your own instructions in **Rules**.

The model is not bundled into the APK because model files are large and have separate licenses. You choose and import a model yourself.

## Privacy
Inference is performed locally by the bundled llama.cpp native runtime. The app does not contain an account, telemetry, or remote AI API integration.
