import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

abstract class StageModelAssets : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val packageFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun stageAssets() {
        fileSystem.sync {
            into(outputDirectory)
            from(packageFiles) { into("general_model") }
        }
    }
}

val mobileModelDirectory = rootProject.file("../models/mobile/dataset_v1-xgboost-fold-1-v1")
val stageModelAssets = tasks.register<StageModelAssets>("stageModelAssets") {
    packageFiles.from(mobileModelDirectory.resolve("model.onnx"),
        mobileModelDirectory.resolve("manifest.json"), mobileModelDirectory.resolve("feature_schema.json"))
    outputDirectory.set(layout.buildDirectory.dir("generated/modelAssets/main"))
}
val stageGoldenSamples = tasks.register<StageModelAssets>("stageGoldenSamples") {
    packageFiles.from(mobileModelDirectory.resolve("golden_samples.json"))
    outputDirectory.set(layout.buildDirectory.dir("generated/modelAssets/androidTest"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(stageModelAssets, StageModelAssets::outputDirectory)
        variant.androidTest?.sources?.assets?.addGeneratedSourceDirectory(
            stageGoldenSamples, StageModelAssets::outputDirectory
        )
    }
}

android {
    namespace = "com.group8.wellbeing"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.group8.wellbeing"
        minSdk = 28
        targetSdk = 37
        versionCode = 2
        versionName = "0.8.0-m11-m12"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.onnxruntime.android)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.ui.tooling)
}
