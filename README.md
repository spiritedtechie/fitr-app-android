Setup
-----

1. Goto the Google API console (https://console.developers.google.com/apis/library)

2. Create a new project (Fitr)

3. Enable the Google Fit APIs

4. Create new OAuth client ID credentials with Application type: Android

5. Find your debug keystore fingerprint:

    keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

Obviously, you can you another keystore e.g. if you are creating an app release.

6. Add the SHA1 to the OAuth client ID credentials