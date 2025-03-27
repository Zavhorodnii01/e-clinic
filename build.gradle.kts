buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.2") // Correct as a classpath dependency
        // ... other classpath dependencies
    }
}

plugins { // Plugin declarations for the *project*
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // DO NOT put google-services here!
}