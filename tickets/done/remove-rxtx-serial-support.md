# Remove RXTX serial support

Captured: 2026-09-24

## Goal

Remove unsupported serial-port printer, display, and scanner configuration from the POS and its release packages.

## Context

Supported setups use screen, file-backed ESC/POS output, or system printers. RXTX native libraries are obsolete and do not cover the supported deployment architectures. Keep file-backed output such as `epson:file,/dev/usb/lp0`.

## Done when

- The configuration UI no longer offers serial connections or serial-port presets.
- Runtime configuration no longer creates RXTX writers.
- RXTX dependencies, native binaries, and related packaging references are removed.

## Shipped

Removed RXTX and serial-port configuration, communication code, native files, and launcher/package references. File-backed ESC/POS output remains supported through `PrinterWritterFile` and `epson:file,...`.
- File-backed ESC/POS output still builds and passes the relevant checks.
