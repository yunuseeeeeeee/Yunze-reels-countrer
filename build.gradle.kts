plugins {
    id("com.android.application") version "8.5.2"
    id("org.jetbrains.kotlin.android") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
}

android {
    namespace = "com.yunze.reels"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yunze.reels"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDir(layout.buildDirectory.dir("gensrc"))
            res.srcDir(layout.buildDirectory.dir("genres"))
        }
    }
}

val prepareFiles = tasks.register("prepareFiles") {
    doLast {
        val src = layout.buildDirectory.dir("gensrc").get().asFile
        val res = layout.buildDirectory.dir("genres").get().asFile
        src.deleteRecursively()
        res.deleteRecursively()
        src.mkdirs()
        res.mkdirs()
        projectDir.listFiles()!!.filter { it.isFile }.forEach { f ->
            if (f.name.endsWith(".kt")) {
                f.copyTo(File(src, f.name), true)
            } else if (f.name.startsWith("res_")) {
                val parts = f.name.split("_", limit = 3)
                val dir = File(res, parts[1])
                dir.mkdirs()
                f.copyTo(File(dir, parts[2]), true)
            }
        }
    }
}

tasks.configureEach {
    if (name == "preBuild") {
        dependsOn(prepareFiles)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
}
