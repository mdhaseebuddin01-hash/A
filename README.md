# Auris for Android

An offline otoscopy assistant for ENT doctors. Take or choose an ear image and Auris shows the most likely condition with its probability, the top 3 possibilities, and whether the answer is **Confident** or **Needs review**. Doctors record whether Auris was right, and the Accuracy tab tracks real-world agreement against the 95% target.

Everything runs on the phone. No image leaves the device.

## What you need
- The three model files from the Colab notebook (`Auris_train_model.ipynb`): `auris.onnx`, `labels.txt`, `model_info.json`
- Android Studio (free) on a Windows PC or Mac, or Claude Code
- An Android phone with Android 8.0 or newer

## 1. Add the model
Copy the three files into `app/src/main/assets/`. Without them the app still opens, but shows “No model is installed yet”.

## 2. Build the app
1. In Android Studio choose **Open** and select this `auris-android` folder.
2. Wait for the Gradle sync to finish. If Android Studio offers to create the Gradle wrapper or upgrade the Android Gradle Plugin, accept.
3. Choose **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. The APK is saved in `app/build/outputs/apk/debug/`.

## 3. Install it on a phone
Copy the APK to the phone, open it, and allow installing from that source when Android asks. Or connect the phone by USB with USB debugging turned on, and press **Run** in Android Studio.

## How the 95% target is checked
- **Test set.** The notebook tests the model on images it never saw during training, and writes the result into `model_info.json`. The Accuracy tab shows it.
- **Confident mode.** The notebook picks a probability threshold where held-out accuracy reaches 95% at the lower end of its likely range. Above the threshold Auris shows “Confident”; below it, “Needs review”, and the doctor decides.
- **In clinic.** Every verdict a doctor records is counted. After about 100 confirmed cases the agreement figure becomes dependable. Export the log as CSV from the Accuracy tab.

## Privacy
Images are analysed on the phone and never uploaded. The verdict log keeps only dates, labels and probabilities, never images or patient details.

## Before doctors rely on it
- Validate on images from a clinic that supplied none of the training data.
- In India, software that helps with diagnosis is regulated as a medical device by CDSCO. Get regulatory advice before clinical use.
- Collect training images with patient consent and remove identifying details, in line with the Digital Personal Data Protection Act, 2023.
- For Google Play, raise `targetSdk` to the level Play currently requires.

## Project notes
- Kotlin, Jetpack Compose and Material 3; ONNX Runtime for Android 1.27.0; minSdk 26, compileSdk and targetSdk 35.
- Model input: float32 `[1, 3, H, W]`, RGB scaled to 0–1. Normalisation and probability calibration happen inside the model. Output: probabilities in `labels.txt` order.
- The size `H × W` is read from the model, so models trained at 224 or 260 px both work.
- Image-quality hints (dark, glare, blur) are simple checks and never block an analysis.
- This project was written without being compiled. The first Gradle sync may ask for small version updates.

## Files
| File | What it does |
|---|---|
| `Classifier.kt` | Loads the ONNX model and runs it on the phone |
| `ImageLoader.kt` | Opens photos upright and resizes them the way training did |
| `ImageQuality.kt` | Dark, glare and blur hints |
| `CaseLog.kt` | Saves doctor verdicts and calculates the accuracy and its likely range |
| `Conditions.kt` | Names and key signs for the 14 conditions |
| `AurisViewModel.kt` | App state |
| `ui/AnalyseScreen.kt`, `ui/ConditionsScreen.kt`, `ui/AccuracyScreen.kt` | The three tabs |
