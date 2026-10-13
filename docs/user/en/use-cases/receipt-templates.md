# Configure receipt content

Receipt content is stored in the database as XML resources. The installed POS
does not read the project files directly.

Edit the resources from **Administration -> Resources**, or edit the files in
`src-pos/com/openbravo/pos/templates/` as a reference.

## Main resources

| Resource | Use |
| --- | --- |
| `Printer.Ticket` | Complete receipt after a sale |
| `Printer.TicketPreview` | Preview and reprint from the sales screen |
| `Printer.TicketGift` | Gift receipt |
| `Printer.TicketLine` | Line sent to the customer display |
| `Printer.Ticket2` | Alternative output when the full receipt is not printed |
| `Printer.TicketTotal` | Total shown during payment |
| `Printer.Ticket.Logo` | Receipt logo image |

Keep `Printer.Ticket`, `Printer.TicketPreview`, and `Printer.TicketGift` in
sync. Otherwise the normal receipt, preview, and gift receipt can have
different formats.

## XML structure

Every template has an `output` root. `display` is the customer display and
`ticket` is the printed receipt:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<output>
    <display>
        <line>
            <text align="center" length="20">Thank you.</text>
        </line>
    </display>
    <ticket>
        <line>
            <text align="center" length="42">Example shop</text>
        </line>
    </ticket>
</output>
```

## Lines and text

Use one `line` element for each printed line:

```xml
<line>
    <text align="left" length="30">Product</text>
    <text align="right" length="12">10.00</text>
</line>
```

`text` supports these attributes:

| Attribute | Values | Meaning |
| --- | --- | --- |
| `align` | `left`, `center`, `right` | Text alignment |
| `length` | Integer | Column width in characters |
| `bold` | `true` | Bold text |
| `underline` | `true` | Underlined text |

The usual receipt width is 42 characters. Keep the sum of column widths within
that width. Use `<line></line>` for a blank line and `size="1"` for a larger
line:

```xml
<line size="1">
    <text bold="true">TOTAL</text>
</line>
```

## Velocity values

The POS evaluates templates with Velocity. Expressions use `${...}`.

### Receipt values

```xml
<text>${ticket.printId()}</text>
<text>${ticket.printDateShort()}</text>
<text>${ticket.printDate()}</text>
<text>${ticket.printUserFirstName()}</text>
<text>${ticket.printCustomer()}</text>
<text>${ticket.printArticlesCount()}</text>
<text>${ticket.printSubTotal()}</text>
<text>${ticket.printTax()}</text>
<text>${ticket.printTotal()}</text>
<text>${ticket.printTotalPaid()}</text>
```

### Product lines

```xml
#foreach ($ticketline in $ticket.getLines())
<line>
    <text align="left" length="42">${ticketline.printTicketName()}</text>
</line>
<line>
    <text align="left" length="25">${ticketline.printTicketReference()}</text>
    <text align="right" length="7">${ticketline.printPriceTaxAmount()}</text>
    <text align="right" length="3">${ticketline.printMultiply()}</text>
    <text align="right" length="7">${ticketline.printValueAmount()}</text>
</line>
#end
```

Common line values include `printName()`, `printPrice()`, `printTax()`,
`printValue()`, and `printGiftVoucherCodes()`.

Use `#if` for optional content:

```xml
#if ($ticketline.hasGiftVoucherCodes())
<line>
    <text bold="true">Code: ${ticketline.printGiftVoucherCodes()}</text>
</line>
#end
```

### Payments

```xml
#foreach ($paymentline in $ticket.payments)
#if ($paymentline.name == "cash")
<line>
    <text>Cash</text>
</line>
<line>
    <text align="left" length="22">Paid:</text>
    <text align="right" length="20">${paymentline.printPaid()}</text>
</line>
<line>
    <text align="left" length="22">Change:</text>
    <text align="right" length="20">${paymentline.printChange()}</text>
</line>
#end
#end
```

Current payment names are `cash`, `cashrefund`, `magcard`, `magcardrefund`,
`cheque`, `chequerefund`, `paperin`, `paperout`, `free`, and `debt`.

## Receipt barcode

Place the barcode inside `ticket`, but not inside `line`:

```xml
#if ($ticket.printId() != "")
<barcode type="CODE128" position="bottom">${ticket.printBarcodeId()}</barcode>
#end
```

`position="bottom"` prints the readable value below the bars. Sales use
`T<id>`, for example `T123`; refunds use `R<id>`. Use `printBarcodeId()` rather
than `printId()` so scanning the barcode opens the receipt options.

## Images and drawer

The logo is another database resource:

```xml
<image>Printer.Ticket.Logo</image>
```

Open the drawer only for cash payments:

```xml
#foreach ($paymentline in $ticket.payments)
#if ($paymentline.name == "cash" || $paymentline.name == "cashrefund")
<opendrawer/>
#end
#end
```

## Applying changes

1. Export or copy the current resources before editing them.
2. Close the POS before changing an embedded Derby database.
3. Edit the resource under **Administration -> Resources**. The files in
   `src-pos/com/openbravo/pos/templates/` are format references only; editing
   them does not update an existing installation.
4. Save the resource and restart the POS.
5. Print a preview and a test receipt.

For raw ESC/POS printer connections, see the repository guide
`escpos-printer.md`.

## Troubleshooting

- **Changing the project XML has no effect:** the POS uses the database copy of
  the resource.
- **Preview differs from the printed receipt:** update `Printer.TicketPreview`
  as well as `Printer.Ticket`.
- **The gift receipt has no barcode:** update `Printer.TicketGift`.
- **Text prints but bars do not:** check that the printer is configured as
  `epson` or `tmu220`, not `printer`.
- **Scanning does not open receipt options:** check that the value is `T<id>`
  or `R<id>` and that the installed POS includes `printBarcodeId()`.
