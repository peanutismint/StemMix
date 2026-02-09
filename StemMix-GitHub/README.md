# StemMix

Real-time AI-powered stem separation for Android. Capture audio from Spotify, YouTube, or any music app, separate it into vocals, drums, bass, and instruments, then control the mix in real-time.

## Features

- 🎵 Real-time audio capture from any Android app
- 🤖 AI-powered stem separation (Spleeter models)
- 🎛️ Professional mixer interface
- 📥 In-app model downloading
- 🎹 Pitch shifting & tempo control
- 🎨 Colorful audio visualizer

## Download

Check the [Releases](../../releases) page for pre-built APKs, or download from [GitHub Actions](../../actions) artifacts.

## Requirements

- Android 10 or higher
- ~150-200MB storage for AI models
- Recommended: Snapdragon 855 or newer

## Installation

1. Download the APK from Releases or Actions
2. Install on your Android device (enable "Install from unknown sources")
3. Grant audio capture permissions
4. Download a stem separation model from Settings
5. Start capturing!

## How to Use

1. Open StemMix
2. Go to Settings → Manage AI Models
3. Download Spleeter 4-stem (~150MB)
4. Tap "Use This Model"
5. Return to main screen
6. Tap "Start Capture"
7. Open Spotify/YouTube and play music
8. Switch back to StemMix to control the mix!

## Building from Source

```bash
git clone https://github.com/YOUR_USERNAME/StemMix.git
cd StemMix
./gradlew assembleDebug
```

APK will be in `app/build/outputs/apk/debug/`

## License

This project is for educational and personal use.

## Credits

- Spleeter models by Deezer Research
- TensorFlow Lite by Google
