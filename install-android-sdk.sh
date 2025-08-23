#/usr/bin/env bash
# SDK downloader for android integration tests
export ANDROID_HOME=$PWD/android-sdk
mkdir -p $ANDROID_HOME/cmdline-tools

# Download and install command line tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O tools.zip
unzip tools.zip -d $ANDROID_HOME/cmdline-tools
mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest

# Install SDK components
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
# set in local properties
echo sdk.dir=./android-sdk >> local.properties