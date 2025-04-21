# CSE-3310-PHMS
To run the project on your computer get the service key for yourself from firebase service accounts and name the file "serviceAccountKey.json" and place it in "PHMS-Backend/src/main/resources/serviceAccountKey.json" 
And also get your "google-services.json" from firebased for the project and place it in "CSE-3310-PHMS/PHMS-Android/app"

make sure you have all these then run ./gradlew run in hte PHMS-Backend folder to run the server first then start your android app with android studio

To test biometric login on your virtual device, first add a fingerprint through the device’s settings (typically found under Security > Fingerprint Enrollment) before you register or log in. Then, on the login screen, open the Extended Controls (the “...” menu on the emulator) and navigate to the Fingerprint section. Use this panel to simulate a fingerprint scan, which will trigger the biometric authentication process in your app

Backend Environment Variables (.env):

The backend uses a .env file to store credentials for sending email alerts (e.g., vital sign alerts).
Create a file named .env in the root directory of the backend project (PHMS-Backend/.env).
Add the following lines to this file, replacing the placeholder values with your actual Gmail email and a generated App Password:
Code snippet

GMAIL_EMAIL=your_gmail_address@gmail.com
GMAIL_APP_PASSWORD=your_16_digit_app_password
How to get GMAIL_APP_PASSWORD:
You cannot use your regular Gmail password here. You need an "App Password".
Go to your Google Account management page (myaccount.google.com).
Navigate to Security.
Ensure 2-Step Verification is turned ON. App Passwords require this.
Under "Signing in to Google", find and click on App passwords. You might need to sign in again.
Select "Mail" for the app and "Other (Custom name)" for the device. Give it a name (e.g., "PHMS Backend").
Click "Generate". Google will provide a 16-character password (like xxxx xxxx xxxx xxxx).
Copy this 16-character password (without spaces) and paste it as the value for GMAIL_APP_PASSWORD in your .env file. Save this password somewhere safe, as Google won't show it again.