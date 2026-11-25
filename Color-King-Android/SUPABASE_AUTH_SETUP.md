# Color King phone authentication setup

The Android app now includes Supabase Phone Auth:
- Register with an Indian mobile number and password.
- Supabase sends an SMS OTP; verify it to complete registration.
- Later sign in using the same mobile number and password.
- Sessions are stored locally and refreshed through Supabase Auth.
- Passwords are sent only to Supabase Auth over HTTPS; the app does not store passwords.

## One-time owner setup (required before real SMS works)

1. Create a project at https://supabase.com/dashboard.
2. In Project Settings > API, copy the Project URL and anon/public key.
3. Edit `app/src/main/assets/auth-config.js`:
   - Set `url` to the Project URL (for example, `https://abcdefgh.supabase.co`).
   - Set `anonKey` to the anon/public key. Never use the service_role key.
4. In Authentication > Sign In / Providers, enable Phone authentication and password sign-in.
5. Configure an SMS provider in Supabase Auth > SMS (for example Twilio or another supported provider), including its credentials and message template.
6. Enable phone confirmation so sign-up requires SMS OTP.
7. Rebuild the APK from GitHub Actions and test with a phone number you control.

Supabase SMS delivery may require a paid SMS provider and applicable country/anti-abuse settings. Until the project URL, anon key, and SMS provider are configured, the app deliberately blocks login and displays a setup message. This is not a fake/local OTP system.
