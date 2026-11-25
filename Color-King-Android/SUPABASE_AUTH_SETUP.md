# Color King phone authentication setup

The Android app supports:
- Register with an Indian mobile number and password, then verify the SMS OTP.
- Log in to an existing account using mobile number + SMS OTP (passwordless).
- Supabase Auth sessions are stored locally and refreshed through Supabase Auth.
- Passwords are sent only to Supabase Auth over HTTPS; the app does not store passwords.

## One-time owner setup required for real SMS delivery

1. Open the ColorKing project in https://supabase.com/dashboard.
2. Go to Authentication > Sign In / Providers and enable Phone authentication.
3. In Authentication > SMS (or the SMS provider settings), configure a supported SMS provider such as Twilio, including its credentials and message template. Provider secrets must be entered only in the Supabase dashboard, never in the Android app or GitHub.
4. Enable phone confirmation if you want registration to require OTP verification.
5. Ensure your SMS provider supports delivery to India (+91) and that any required sender ID, template, balance, and country permissions are configured.
6. Rebuild the APK from GitHub Actions and test with a phone number you control.

The app's LOGIN tab sends an OTP for an existing phone account (it does not create a new account). REGISTER sends an OTP and creates a phone account after verification. Real SMS delivery cannot work until the SMS provider is configured in Supabase; a publishable key and project URL alone are not enough.
