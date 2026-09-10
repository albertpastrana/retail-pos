#!/usr/bin/env python3
"""Stream ESC/POS bytes from a FIFO to a USB Epson receipt printer.

The POS can only write a file or a serial port (machine.printer=epson:file,<path>).
On current macOS, a USB TM-T88-class printer often enumerates as vendor-class USB
(not a CUPS printer and not /dev/cu.*). This helper is the missing piece: it is not
started by the POS. Run it on the till machine, or install it as a user LaunchAgent.

    python3 -m venv ~/.venvs/escpos
    ~/.venvs/escpos/bin/pip install pyusb
    brew install libusb   # macOS

    ~/.venvs/escpos/bin/python tools/escpos-usb-bridge.py --list
    ~/.venvs/escpos/bin/python tools/escpos-usb-bridge.py --fifo /tmp/escpos
"""

import argparse
import os
import stat
import sys
import time

os.environ.setdefault("DYLD_FALLBACK_LIBRARY_PATH", "/opt/homebrew/lib")

import usb.core
import usb.util

EPSON_VENDOR = 0x04B8


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fifo", default="/tmp/escpos", help="FIFO the POS writes to")
    parser.add_argument(
        "--vendor",
        type=lambda v: int(v, 0),
        default=EPSON_VENDOR,
        help="USB vendor id (default: Epson 0x04b8)",
    )
    parser.add_argument(
        "--product",
        type=lambda v: int(v, 0),
        default=None,
        help="USB product id; omit to accept the only matching printer",
    )
    parser.add_argument(
        "--serial", default=None, help="USB serial number, if several printers match"
    )
    parser.add_argument(
        "--endpoint",
        type=lambda v: int(v, 0),
        default=None,
        help="bulk OUT endpoint; found automatically when omitted",
    )
    parser.add_argument(
        "--list", action="store_true", help="print matching USB devices and exit"
    )
    parser.add_argument(
        "--poll",
        type=float,
        default=2.0,
        help="seconds between retries when the printer is unplugged",
    )
    return parser.parse_args()


def bulk_out_endpoint(device, preferred=None):
    try:
        cfg = device.get_active_configuration()
    except usb.core.USBError:
        device.set_configuration()
        cfg = device.get_active_configuration()
    if preferred is not None:
        return preferred
    for intf in cfg:
        endpoint = usb.util.find_descriptor(
            intf,
            custom_match=lambda ep: usb.util.endpoint_direction(ep.bEndpointAddress)
            == usb.util.ENDPOINT_OUT
            and usb.util.endpoint_type(ep.bmAttributes) == usb.util.ENDPOINT_TYPE_BULK,
        )
        if endpoint is not None:
            return endpoint.bEndpointAddress
    return None


def describe(device):
    try:
        manufacturer = device.manufacturer
    except Exception:
        manufacturer = "?"
    try:
        product = device.product
    except Exception:
        product = "?"
    try:
        serial = device.serial_number
    except Exception:
        serial = "?"
    return manufacturer, product, serial


def iter_candidates(vendor, product, serial):
    kwargs = {"find_all": True, "idVendor": vendor}
    if product is not None:
        kwargs["idProduct"] = product
    found = usb.core.find(**kwargs)
    for device in found:
        manufacturer, name, serial_number = describe(device)
        endpoint = bulk_out_endpoint(device)
        if endpoint is None:
            continue
        if serial is not None and serial_number != serial:
            continue
        yield device, endpoint, manufacturer, name, serial_number


def list_devices(vendor, product, serial):
    rows = list(iter_candidates(vendor, product, serial))
    if not rows:
        print("no matching USB printers (need a bulk OUT endpoint)", file=sys.stderr)
        return 1
    for device, endpoint, manufacturer, name, serial_number in rows:
        print(
            f"{device.idVendor:#06x}:{device.idProduct:#06x}  "
            f"{manufacturer} {name}  serial={serial_number}  endpoint={endpoint:#04x}"
        )
    return 0


def pick_device(vendor, product, serial, preferred_endpoint, poll):
    while True:
        rows = list(iter_candidates(vendor, product, serial))
        if len(rows) == 1:
            device, endpoint, manufacturer, name, serial_number = rows[0]
            if preferred_endpoint is not None:
                endpoint = preferred_endpoint
            try:
                device.set_configuration()
            except usb.core.USBError:
                pass
            print(
                f"{manufacturer} {name} serial={serial_number} "
                f"{device.idVendor:#06x}:{device.idProduct:#06x} endpoint={endpoint:#04x}",
                flush=True,
            )
            return device, endpoint
        if len(rows) > 1:
            print(
                "several matching printers; pass --product and/or --serial:",
                file=sys.stderr,
            )
            for device, endpoint, manufacturer, name, serial_number in rows:
                print(
                    f"  {device.idVendor:#06x}:{device.idProduct:#06x}  "
                    f"{manufacturer} {name}  serial={serial_number}",
                    file=sys.stderr,
                )
            sys.exit(2)
        time.sleep(poll)


def ensure_fifo(path):
    if os.path.exists(path):
        if not stat.S_ISFIFO(os.stat(path).st_mode):
            sys.exit(f"{path} exists and is not a FIFO")
        return
    os.mkfifo(path, 0o666)


def main():
    args = parse_args()
    if args.list:
        sys.exit(list_devices(args.vendor, args.product, args.serial))

    ensure_fifo(args.fifo)
    device, endpoint = pick_device(
        args.vendor, args.product, args.serial, args.endpoint, args.poll
    )
    print(f"waiting for tickets on {args.fifo}", flush=True)

    while True:
        with open(args.fifo, "rb") as tickets:
            while True:
                chunk = tickets.read(4096)
                if not chunk:
                    break
                try:
                    device.write(endpoint, chunk, 5000)
                except usb.core.USBError as error:
                    print(f"write failed: {error}", file=sys.stderr, flush=True)
                    device, endpoint = pick_device(
                        args.vendor, args.product, args.serial, args.endpoint, args.poll
                    )


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        pass
