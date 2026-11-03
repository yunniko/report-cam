# stub-messenger

A test tool, not part of Report Cam. It builds one tiny APK per quick messenger, each installed under
that messenger's real package name and answering `ACTION_SEND` for images. With them on an emulator,
the review screen shows the quick buttons as it would on a phone with those apps, and each send can be
checked: the stub logs the caption and the number of photo bytes it could read.

    ./gradlew.bat :stub-messenger:assembleWhatsappDebug
    adb install -r stub-messenger/build/outputs/apk/whatsapp/debug/stub-messenger-whatsapp-debug.apk
    adb logcat -s StubMessenger

Never install these on a phone that has the real apps: they share the package names.
