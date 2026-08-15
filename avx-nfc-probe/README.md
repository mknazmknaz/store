# AVX NFC Probe

Android diagnostic app for checking an authorised physical NFC card and whether an existing contactless access reader routes ISO-DEP/APDU traffic to Android HCE.

## Modes

### Card Inspector
Reads the physical tag UID presented by Android and lists NFC technologies. It does not change or duplicate the credential.

### HCE Reader Probe
Registers `HostApduService`, advertises AVX test AID `F0010203040506` and NFC Forum Type 4 NDEF AID `D2760000850101`, and logs APDUs routed by Android. It does not set/clone a MIFARE Classic UID or send unlock commands.

## One-app test
1. Open app and turn NFC on.
2. Tap **Scan Card** and scan the authorised physical card.
3. Tap **Probe Reader**.
4. Keep phone unlocked and tap Falco for 2–3 seconds.
5. Check whether **APDU received** rises above 0.
6. Repeat on Micro ID.

**APDU > 0** means the reader selected an AID routed to the HCE service. **APDU = 0** means this HCE service was not reached; that is consistent with legacy UID/MIFARE handling but is not absolute proof of no ISO-DEP capability because HCE routing is AID-based.
