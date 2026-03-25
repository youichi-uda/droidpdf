plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint")
    `maven-publish`
}

android {
    namespace = "com.droidpdf"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += listOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/NOTICE")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // FontBox for font parsing/subsetting
    implementation("org.apache.pdfbox:fontbox:3.0.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.11.4")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("com.google.truth:truth:1.4.4")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.youichi-uda"
                artifactId = "droidpdf"
                version = "1.0.0"

                pom {
                    name.set("DroidPDF")
                    description.set("Kotlin-native PDF library for Android")
                    url.set("https://droidpdf.abyo.net")
                    licenses {
                        license {
                            name.set("Business Source License 1.1")
                            url.set("https://github.com/youichi-uda/droidpdf/blob/main/LICENSE")
                        }
                    }
                }
            }
        }
    }
}
