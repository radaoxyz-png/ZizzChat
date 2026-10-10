plugins {
    // ... existing plugins
    id 'com.google.gms.google-services'
}

dependencies {
    // ... existing dependencies

    // Firebase BoM & Libraries
    implementation platform('com.google.firebase:firebase-bom:33.1.0')
    implementation 'com.google.firebase:firebase-auth-ktx'
    implementation 'com.google.firebase:firebase-firestore-ktx'
}
