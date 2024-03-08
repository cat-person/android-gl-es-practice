plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    group = "cafe.serenity.descriptor_renderer"
    version = "1.0"

    namespace = "cafe.serenity.descriptor_renderer"
    compileSdk = 33

    defaultConfig {
        minSdk = 27

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("description-renderer") {
                groupId = "com.github.cat-person"
                artifactId = "description-renderer"
                version = "1.0"

                from(components["release"])
            }
        }
    }
}