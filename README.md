# MAJAL Mobile

Android companion app for managing company VPN profiles and network operations.

## Current VPN integration prototype

- Register multiple companies locally on the Android device.
- Select a `.ovpn` file for each company.
- Hand the selected profile to **OpenVPN for Android** for import.
- Start and stop a named OpenVPN for Android profile from MAJAL Mobile using its documented external activity API.
- OpenVPN for Android handles authentication (username/password, certificates, or mixed profile requirements); MAJAL Mobile does not store VPN passwords or private keys.

## Important setup

1. Install **OpenVPN for Android** by Arne Schwabe (package: `de.blinkt.openvpn`).
2. In MAJAL Mobile, add a company and select its `.ovpn` file.
3. Complete the import in OpenVPN for Android.
4. Set the imported profile name to exactly match the company name shown in MAJAL Mobile.
5. Return to MAJAL Mobile, tap the confirmation button, then tap Connect.
6. Verify the actual connection state in Android's VPN notification / OpenVPN app.

This is an integration with a separately installed OpenVPN client, not an embedded VPN engine. The app sends connect/disconnect requests; it does not yet receive live connection-state callbacks. One Android VPN connection is normally active per user at a time.

## Security

Never commit customer `.ovpn` files, credentials, private keys, certificates, server addresses, or internal network details to this public repository. Keep those only on the user's device and import them through the app.

## Current limitations

- VPN profile import and connect/disconnect handoff are the first integration milestone.
- Live VPN status synchronization, device polling (MikroTik/Cisco/ESXi), alerts, backups, and configuration management are not implemented yet.
