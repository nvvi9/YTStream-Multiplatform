plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.multiplatform).apply(false)
    alias(libs.plugins.kotlinx.serialization).apply(false)
}
