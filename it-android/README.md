# Android integration tests

Rhino runs with some restrictions on android platform. This project builds a very minimalistic app/testframework to run
android tests either in an emulator or directly on your phone.

The goal of these tests is to catch over time the kinds of incompatibilities that
arise when we use APIs that are not supported on every Android SDK version.

Android developers who want to ensure that specific cases work are encouraged to
add new tests to the test case directory:

    ./it-android/src/main/assets/tests

## Restrictions

### Rhino runtime

- minSdk is 26 (see #1785 for background)
- runs only in interpreted mode
- not all features may be available (JavaAdapter, ...)

### Emulator

- runs in emulated mode and may behave different, than a real android-device (JIT etc.)

## Testing

### Tests

Write your tests in `src/main/assets/tests` in the same style as MozillaTestSuite

These are simple javascript tests.
(To write java-tests it is recommended to use android-studio.)

### Running tests on github

This is done automatically on each PR.

### Running tests locally

#### Setting up the SDK

You need an Android SDK installed in order for the tests to run

- Either install Android-Studio with SDK https://developer.android.com/about/versions/14/setup-sdk

- make sure that your `ANDROID_HOME` enviroment is set up properly
- Or, use the provided script in `<RHINO_ROOT>/install-android-sdk`

  This will install the SDK in `<RHINO_ROOT>/android-sdk` and register it in the `local.properties` for this gradle
  build only. (This is the best option, if you do not want to mess up your system)

  <b>Note:</b> The script will automatically accept all license terms!

#### Starting an Emulator: Option 1

On some environments, you can use `<RHINO_ROOT>/run-android-tests-locally.sh` to start an emulator in docker/podman, run the tests and install the APK. This test may not work in every case, but is an option on Linux for sure.

This will download an emulator based on https://github.com/budtmo/docker-android and you can access the emulated phone
on http://localhost:6080

<b>Note:</b> The emulator will not terminate automatically. You will need to remove it from your docker/podman manually

#### Starting the Emulator: Option 2

Once the SDK is installed, you can download Android Studio and use the "Device Manager" in the UI to
start an emulator.

Once you have started the emulator, if you run:

    ./gradlew :it-android:connectedAndroidTest

it will connect to the emulator, install the test app, and run the tests.

### Debugging and troubleshooting

- `./gradlew it-android:connectedAndroidTest` will try to run the tests automatically on the connected android device
- `./android-sdk/platform-tools/adb logcat` get logs from the device
- `./android-sdk/platform-tools/adb devices` list devices
- `./android-sdk/platform-tools/adb disconnect` disconnect devices
- `./android-sdk/platform-tools/adb connect localhost:5555` reconnect to emulator
- `docker rm -f android-container` or `podman rm -f android-container` remove the container

### Running tests on your phone

You need an Android SDK and an Android phone of course. (iPhone will not work here)

- open your settings and enable `wireless debugging`
- select `pari device with pariing code`
- run `./android-sdk/platform-tools/adb pair HOST:PORT PAIRING_CODE`
- run `./android-sdk/platform-tools/adb connect HOST:PORT` (Note: The port is different from the pairing port)
- check `./android-sdk/platform-tools/adb devices`, if your device is connected
- run `./gradlew it-android:connectedAndroidTest` to run your tests
- or run `./gradlew it-android:installDebug` to deploy the app on your phone

