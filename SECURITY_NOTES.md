# Security Notes for PocketCodeAgent

## API Key Handling

- **Never** show API keys in demo videos, screenshots, or screen recordings.
- If an API key is accidentally exposed in a public repository or video, it must be **rotated immediately**.

## Storage and Logging

- PocketCodeAgent stores API keys locally and securely (encrypted in the Android Keystore).
- Under no circumstances should API keys, Authorization headers, or full sensitive request bodies be written to log files, console output (`Log.d`, `println`), Error messages, or Toasts.
- When an API request fails, error messages must be sanitized and never display raw response bodies if they have the potential to contain repeated secrets or internal tokens.
- Debug-Logs must be free of any secrets.
