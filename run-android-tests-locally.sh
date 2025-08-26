#/usr/bin/env bash
CONTAINER_NAME=android-container
# See https://github.com/budtmo/docker-android for available container images
IMAGE=budtmo/docker-android:emulator_13.0
HTTP_PORT=6080
ADB_PORT=5555
APP=app/build/outputs/apk/debug/app-debug.apk
CONTAINER_CMD=$(command -v docker)
if [ -z "$CONTAINER_CMD" ]; then
  CONTAINER_CMD=$(command -v podman)
fi
if [ -z "$CONTAINER_CMD" ]; then
  echo "no docker or podman installation found"
  exit 1
fi

echo "Trying to start '$CONTAINER_NAME' on http://localhost:$HTTP_PORT and install $APP (using $CONTAINER_CMD)"
if $CONTAINER_CMD ps --filter "name=$CONTAINER_NAME" --filter "status=running" --format "{{.Names}}" | grep -wq "$CONTAINER_NAME"; then
    echo "- Container is already running - re-installing app (to remove container, use '$CONTAINER_CMD rm -f $CONTAINER_NAME')"
else
    echo "- Container is not running. Starting '$CONTAINER_NAME'"
    $CONTAINER_CMD run -d \
      -p $HTTP_PORT:6080 \
      -p $ADB_PORT:5555 \
      -e EMULATOR_DEVICE="Samsung Galaxy S10" \
      -e WEB_VNC=true \
      -v $PWD:/home/androidusr/tmp \
      -w /home/androidusr/tmp \
      --device /dev/kvm \
      --name $CONTAINER_NAME \
      $IMAGE
fi

while ! ($CONTAINER_CMD exec -it $CONTAINER_NAME adb shell getprop sys.boot_completed | grep -wq 1) 2>/dev/null ; do
  echo "- Waiting for emulator to boot..."
  sleep 10
done
./gradlew it-android:connectedAndroidTest
./gradlew it-android:installDebug
echo "Open http://localhost:$HTTP_PORT in your browser and check result"