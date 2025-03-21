# CSE-3310-PHMS
To run the project on your computer get the service key for yourself from firebase service accounts and name the file "serviceAccountKey.json" and place it in "PHMS-Backend/src/main/resources/serviceAccountKey.json" 
And also get your "google-services.json" from firebased for the project and place it in "CSE-3310-PHMS/PHMS-Android/app"

make sure you have all these then run ./gradlew run in hte PHMS-Backend folder to run the server first then start your android app with android studio

To test biometric login on your virtual device, first add a fingerprint through the device’s settings (typically found under Security > Fingerprint Enrollment) before you register or log in. Then, on the login screen, open the Extended Controls (the “...” menu on the emulator) and navigate to the Fingerprint section. Use this panel to simulate a fingerprint scan, which will trigger the biometric authentication process in your app.