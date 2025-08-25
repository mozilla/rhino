#/usr/bin/env bash
CONTAINER_NAME=android-container
IMAGE=budtmo/docker-android:emulator_13.0
HTTP_PORT=6080
ADB_PORT=5555
APP=app/build/outputs/apk/debug/app-debug.apk


echo "Build was successful, trying to start '$CONTAINER_NAME' on http://localhost:$HTTP_PORT and install $APP"
if docker ps --filter "name=$CONTAINER_NAME" --filter "status=running" --format "{{.Names}}" | grep -wq "$CONTAINER_NAME"; then
    echo "- Container is already running - re-installing app (to remove container, use 'docker rm -f $CONTAINER_NAME')"
else
    echo "- Container is not running. Starting '$CONTAINER_NAME'"
    docker run -d \
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

while ! (docker exec -it $CONTAINER_NAME adb shell getprop sys.boot_completed | grep -wq 1) 2>/dev/null ; do
  echo "- Waiting for emulator to boot..."
  sleep 10
done
#echo "- Emulator booted"
#docker exec -it $CONTAINER_NAME adb uninstall com.example.rhino
#docker exec -it $CONTAINER_NAME adb install -r -t $APP && \
#docker exec -it $CONTAINER_NAME adb shell am start -n com.example.rhino/com.example.rhino.MainActivity
./gradlew it-android:connectedAndroidTest
./gradlew it-android:installDebug
echo "Open http://localhost:$HTTP_PORT in your browser and check result"