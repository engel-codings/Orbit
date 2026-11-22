plugins {
    kotlin("multiplatform")
}

// Web-Version von Orbit (für iPhone, iPad und PC) – nutzt denselben Spiel-Kern wie Android.
kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "orbit.js"
            }
        }
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            kotlin.srcDir("../shared/src")
        }
    }
}

// Schriften aus der Android-App übernehmen (nur einmal im Projekt gespeichert)
tasks.named<Copy>("jsProcessResources") {
    from("../app/src/main/assets/fonts") { into("fonts") }
    filesMatching("sw.js") {
        filter { it.replace("__VERSION__", System.currentTimeMillis().toString()) }
    }
}
