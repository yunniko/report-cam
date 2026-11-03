// A test tool, never shipped: an app that answers ACTION_SEND under a real messenger's package name, so
// the review screen's quick buttons can be checked on an emulator that has no messengers. One flavour
// per messenger in share/Messenger.kt. See stub-messenger/README.md.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "cz.julienika.stubmessenger"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1"
    }

    flavorDimensions += "messenger"
    productFlavors {
        mapOf(
            "whatsapp" to "com.whatsapp",
            "telegram" to "org.telegram.messenger",
            "messenger" to "com.facebook.orca",
            "discord" to "com.discord",
            "slack" to "com.Slack",
            "snapchat" to "com.snapchat.android",
            "instagram" to "com.instagram.android",
        ).forEach { (name, pkg) ->
            create(name) {
                dimension = "messenger"
                applicationId = pkg
                resValue("string", "stub_label", "Stub $name")
            }
        }
    }

    buildFeatures {
        resValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
