# Phone→TV sync via LAN/QR Canvas Pairing

Auto shares the phone process; TV is a separate device. Cloud accounts for v1 would contradict “configure on phone” without adding auth scope. **Canvas Pairing** pushes a **manifest** for Remote Sources and **blobs** for Personal Photos (and missing Bundled Pack pieces). The phone is not a live media server for the TV session.

## UX (no IP typing)

- **TV**: hosts HTTP on LAN (`:8742`), advertises via NSD (`_arthur-pairing._tcp.`), shows a large QR encoding `arthur://pair?host=<ipv4>&port=8742`.
- **Phone**: discovers TVs via NSD (one-tap Sync) and/or scans the QR in-app (Google Code Scanner). Deep links with a host auto-push.
- Custom scheme only (`arthur://`) — no website/App Links dependency for pairing.
