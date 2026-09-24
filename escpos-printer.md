# ESC/POS receipt printers

The POS does not talk to CUPS or the Windows spooler for tickets. It writes **raw ESC/POS bytes** to a file or device path. Set this in Configuration → General, or in the properties file:

```properties
machine.printer=epson:file,<path>
```

Use type **epson** for thermal TM-T printers (TM-T88, TM-T20, …). Use **tmu220** only for a TM-U220 impact printer. Type **printer** goes through the OS driver and is the wrong path for cutter and cash-drawer commands.

The output path is editable. Use a regular file, FIFO, or operating-system device path.

## Windows

USB is not LPT1. Either:

- Write straight to the USB printing port: `epson:file,\\.\USB001` (or `USB002`). Remove or disable the Epson Windows driver if it locks the port.
- Or install **Generic / Text Only**, share it, map the share, and use `LPT1`:

```bat
net use LPT1 \\%COMPUTERNAME%\EPSON /persistent:yes
```

```properties
machine.printer=epson:file,LPT1
```

The share + LPT1 path goes through the spooler and is often slow. Prefer `\\.\USB001` when it works. In printer properties, turn off bidirectional support and choose **Print directly to the printer**.

## Linux

The kernel device is usually `/dev/usb/lp0`. The till user needs the `lp` group.

```properties
machine.printer=epson:file,/dev/usb/lp0
```

## macOS

Current macOS **rejects raw CUPS queues** (`lpadmin: Raw queues are no longer supported on macOS`). A TM-T88IV on USB often shows up as vendor-class USB (`bInterfaceClass 255`): no `/dev/cu.*`, and `lpinfo -v` does not list it.

If `ls /dev/cu.*` shows a `usbserial` / `usbmodem` node, point the POS at that file and stop here. Otherwise run the USB bridge on the till Mac. It is **not** started by the POS; it is a per-machine helper.

### USB bridge (any Mac, same kind of printer)

Needs Homebrew [libusb](https://libusb.info/) and Python [pyusb](https://github.com/pyusb/pyusb).

```sh
brew install libusb
python3 -m venv ~/.venvs/escpos
~/.venvs/escpos/bin/pip install pyusb

~/.venvs/escpos/bin/python tools/escpos-usb-bridge.py --list
```

`--list` prints vendor:product, name, serial, and bulk OUT endpoint. Default vendor is Epson (`0x04b8`). If several printers match, pass `--product` and/or `--serial`.

Then leave this running while the till is open (it waits if the USB cable is unplugged):

```sh
~/.venvs/escpos/bin/python tools/escpos-usb-bridge.py --fifo /tmp/escpos
```

POS config on that Mac:

```properties
machine.printer=epson:file,/tmp/escpos
```

Start the bridge **before** printing. Opening the FIFO for write blocks until a reader exists.

To run it at login without tying it to the app, copy a LaunchAgent onto **that** Mac (paths are local):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>local.escpos-usb-bridge</string>
  <key>ProgramArguments</key>
  <array>
    <string>/Users/YOU/.venvs/escpos/bin/python</string>
    <string>/Users/YOU/pworkspace/openbravopos/tools/escpos-usb-bridge.py</string>
    <string>--fifo</string>
    <string>/tmp/escpos</string>
  </array>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>
  <key>EnvironmentVariables</key>
  <dict>
    <key>PATH</key>
    <string>/opt/homebrew/bin:/usr/bin:/bin</string>
    <key>DYLD_FALLBACK_LIBRARY_PATH</key>
    <string>/opt/homebrew/lib</string>
  </dict>
  <key>StandardOutPath</key>
  <string>/Users/YOU/Library/Logs/escpos-usb-bridge.log</string>
  <key>StandardErrorPath</key>
  <string>/Users/YOU/Library/Logs/escpos-usb-bridge.log</string>
</dict>
</plist>
```

Save as `~/Library/LaunchAgents/local.escpos-usb-bridge.plist`, then:

```sh
launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/local.escpos-usb-bridge.plist
```

On an Intel Mac, Homebrew’s libusb is under `/usr/local/lib` instead of `/opt/homebrew/lib`. If Epson’s Mac driver or TM Virtual Port owns the device, libusb cannot claim it — remove that driver first.

The POS still only needs `epson:file,/tmp/escpos`. Auto-start is a machine setting, not part of the till process.
