# StemMix Setup Guide for Windows

This guide will walk you through setting up Android development on Windows and running the StemMix app.

## Part 1: Installing Android Studio (30-45 minutes)

### Step 1: Download Android Studio

1. Open your web browser and go to: https://developer.android.com/studio
2. Click the big green "Download Android Studio" button
3. Accept the terms and conditions
4. The file is about 1 GB, so it will take a few minutes to download
5. Save it somewhere you can find it (like your Downloads folder)

### Step 2: Install Android Studio

1. Find the downloaded file (should be named something like `android-studio-2023.x.x.xx-windows.exe`)
2. Double-click it to run the installer
3. If Windows asks "Do you want to allow this app to make changes?", click "Yes"
4. Click "Next" on the welcome screen
5. Leave all components checked (Android Studio, Android Virtual Device) and click "Next"
6. Choose install location (default is fine) and click "Next"
7. Click "Install" and wait (this takes 5-10 minutes)
8. Click "Next" when installation completes
9. Check "Start Android Studio" and click "Finish"

### Step 3: Android Studio Setup Wizard

1. On the "Import Android Studio Settings" screen, select "Do not import settings" and click "OK"
2. Click "Next" on the Welcome screen
3. Choose "Standard" installation type and click "Next"
4. Choose your preferred theme (Light or Dark) and click "Next"
5. Click "Next" on the Verify Settings screen
6. Click "Finish" to download components (this takes 15-30 minutes depending on your internet)
7. Once done, click "Finish"

### Step 4: Create an Android Virtual Device (Emulator)

1. On the Welcome screen, click "More Actions" → "Virtual Device Manager"
2. Click "Create Device"
3. Under "Phone" category, select "Pixel 6" (or any device with Play Store icon)
4. Click "Next"
5. On the "System Image" screen:
   - Find "Tiramisu" (API Level 33) - has an Android 13 icon
   - If it says "Download" next to it, click it and wait for download
   - Once downloaded, select it
6. Click "Next"
7. Give it a name like "Pixel 6 API 33" (or leave default)
8. Click "Finish"

You now have Android Studio installed!

## Part 2: Opening and Building the StemMix Project

### Step 1: Extract the Project

1. Find where you saved/downloaded the StemMix project folder
2. If it's in a ZIP file, right-click and choose "Extract All"
3. Remember the location (e.g., `C:\Users\YourName\StemMix`)

### Step 2: Open Project in Android Studio

1. Open Android Studio
2. Click "Open" (or File → Open if already in a project)
3. Navigate to where you extracted StemMix
4. Select the **StemMix folder** (the one containing build.gradle)
5. Click "OK"

### Step 3: Wait for Gradle Sync

1. Android Studio will start "Syncing Gradle"
   - You'll see a progress bar at the bottom
   - This downloads dependencies (first time takes 5-15 minutes)
   - **Do not interrupt this process**
2. If you see any errors about SDK versions:
   - Click "Install missing SDK" or "Update" buttons that appear
   - Accept licenses if prompted
3. Wait until you see "Gradle sync finished" at the bottom

### Step 4: Run on Emulator

1. Make sure the emulator is selected in the device dropdown (top toolbar)
   - Should say something like "Pixel 6 API 33"
2. Click the green "Play" button (or press Shift+F10)
3. The emulator will start (takes 1-2 minutes first time)
   - You'll see an Android phone screen appear
4. The app will install and launch automatically

**⚠️ IMPORTANT LIMITATION:**
The emulator **cannot capture audio** from other apps. The app will open and you can interact with the UI, but the audio processing won't work. To test the actual audio features, you need a real Android phone (see Part 3).

## Part 3: Running on a Real Android Phone (Recommended)

### Prerequisites

- An Android phone with Android 10 or newer
- A USB cable to connect phone to PC
- Your phone's USB debugging enabled

### Step 1: Enable Developer Options on Your Phone

1. On your Android phone, go to **Settings**
2. Scroll down to **About Phone** (or **About Device**)
3. Find **Build Number** (might be under "Software Information")
4. **Tap "Build Number" 7 times**
   - You'll see a message like "You are now a developer!"
5. Go back to main Settings
6. Find **Developer Options** (usually near the bottom or in "System")
7. Turn on the switch at the top to enable Developer Options
8. Scroll down and find **USB Debugging**
9. Turn on USB Debugging
10. If prompted, confirm "Allow USB debugging"

### Step 2: Connect Phone to PC

1. Plug your phone into your PC with a USB cable
2. On your phone, you'll see a prompt: "Allow USB debugging?"
   - Check "Always allow from this computer"
   - Tap "OK" or "Allow"
3. In Android Studio, your phone should now appear in the device dropdown
   - It will show your phone's model name

### Step 3: Run App on Phone

1. Select your phone from the device dropdown
2. Click the green "Play" button
3. The app will install on your phone and launch
4. On first run, the app will ask for permissions - grant them all

### Step 4: Test the App

1. **In StemMix**: Tap "Start Capture"
2. A system dialog appears: "StemMix will start capturing..."
   - Tap "Start now"
3. **Open Spotify or YouTube** on your phone
4. Play some music
5. **Switch back to StemMix** (swipe up and tap the app)
6. You should now be able to:
   - See the visualizer moving
   - Adjust faders to control stems
   - Use mute/solo buttons
   - Change key and tempo

## Common Issues and Solutions

### Issue: "Gradle sync failed"

**Solution:**
1. Click "File" → "Invalidate Caches"
2. Select "Invalidate and Restart"
3. Wait for Android Studio to restart and sync again

### Issue: "SDK location not found"

**Solution:**
1. Click "File" → "Project Structure"
2. Under "SDK Location", click "Edit"
3. Let Android Studio download and set up the SDK

### Issue: "Phone not showing in device list"

**Solutions:**
1. Make sure USB debugging is enabled (see Part 3, Step 1)
2. Try unplugging and replugging the USB cable
3. Try a different USB cable or USB port
4. On phone, change USB mode to "File Transfer" or "MTP"
5. Install phone drivers:
   - Search "YOUR_PHONE_BRAND USB drivers" (e.g., "Samsung USB drivers")
   - Download and install from manufacturer's website

### Issue: App crashes on start

**Solutions:**
1. Make sure your Android version is 10 or higher:
   - Settings → About Phone → Android Version
2. Try uninstalling and reinstalling the app
3. Check LogCat in Android Studio for error messages

### Issue: "No audio processing happening"

**Solutions:**
1. Make sure you clicked "Start Capture" and granted permission
2. Ensure music is actually playing in Spotify/YouTube
3. Try stopping capture and starting again
4. Some apps may block audio capture - try a different music app

### Issue: Audio stuttering or glitching

**Solutions:**
1. Open Settings in the app
2. Increase "Buffer Length" to 3 or 4 seconds
3. Switch to "Spleeter 2-stem" model (faster)
4. Set quality to "Performance"

## Testing Without a Phone

If you don't have an Android phone, you can still explore the UI in the emulator:

1. Run on emulator as described in Part 2
2. The UI will work (faders, buttons, etc.)
3. The visualizer will show random data
4. Audio capture won't work (emulator limitation)

This lets you see the interface and understand how it would work on a real device.

## Next Steps

Once you have the app running:

1. Read the main README.md for usage instructions
2. Experiment with different fader positions
3. Try the mute/solo buttons
4. Tap the visualizer to cycle through styles
5. Adjust key and tempo controls
6. Open Settings to try different models

## Getting Help

If you run into issues not covered here:

1. Check the "Troubleshooting" section in README.md
2. Look at Android Studio's "Logcat" panel (bottom of screen) for error messages
3. Search for your specific error message online
4. Android development has a large community, so most errors have been solved before

## Estimated Time Requirements

- First-time setup: 1-2 hours
- Opening project next time: 2-5 minutes
- Running on emulator: 2 minutes
- Running on phone: 1 minute (after first setup)

Don't worry if it seems slow the first time - after initial setup, everything runs much faster!
