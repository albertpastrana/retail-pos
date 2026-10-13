# Configurar el contingut del tiquet

El contingut dels tiquets es desa a la base de dades com a recursos XML. La
impressora no llegeix directament els fitxers del projecte quan el POS ja està
instal·lat.

Per configurar-los cal tenir permisos d’administració i editar els recursos des
de **Administració → Recursos**. També es poden editar els fitxers de
`src-pos/com/openbravo/pos/templates/` com a referència.

## Recursos principals

| Recurs | Ús |
| --- | --- |
| `Printer.Ticket` | Tiquet complet d’una venda pagada |
| `Printer.TicketPreview` | Previsualització i reimpressió des de la pantalla de vendes |
| `Printer.TicketGift` | Tiquet regal |
| `Printer.TicketLine` | Línia enviada al visor de client |
| `Printer.Ticket2` | Sortida alternativa quan no s’imprimeix el tiquet complet |
| `Printer.TicketTotal` | Total mostrat durant el pagament |
| `Printer.Ticket.Logo` | Imatge del logotip |

Els recursos `Printer.Ticket`, `Printer.TicketPreview` i `Printer.TicketGift`
normalment s’han de mantenir coordinats. Si només se’n modifica un, la
previsualització o el tiquet regal poden tenir un format diferent del tiquet
normal.

## Estructura mínima

Una plantilla és un document XML amb un element arrel `output`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<output>
    <display>
        <line>
            <text align="center" length="20">Gràcies.</text>
        </line>
    </display>

    <ticket>
        <line>
            <text align="center" length="42">Tiquet de venda</text>
        </line>
    </ticket>
</output>
```

`display` és el visor de client. `ticket` és el paper de la impressora.

## Línies i text

Cada `<line>` comença una línia i es tanca amb `</line>`:

```xml
<line>
    <text align="left" length="30">Producte</text>
    <text align="right" length="12">10,00 €</text>
</line>
```

Atributs de `<text>`:

| Atribut | Valors | Significat |
| --- | --- | --- |
| `align` | `left`, `center`, `right` | Alineació del text |
| `length` | Nombre enter | Amplada de la columna en caràcters |
| `bold` | `true` | Negreta |
| `underline` | `true` | Subratllat |

L’amplada habitual del tiquet és de 42 caràcters. La suma de les amplades d’una
línia no hauria de superar aquesta amplada.

Línies buides:

```xml
<line></line>
```

Mida de lletra de la línia:

```xml
<line size="1">
    <text bold="true">TOTAL</text>
</line>
```

## Variables Velocity

Abans d’imprimir, el POS avalua la plantilla amb Velocity. Les expressions es
posen entre `${...}`.

### Dades del tiquet

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

Identificador preparat per a l’escàner de tiquets:

```xml
${ticket.printBarcodeId()}
```

Retorna `T123` per una venda i `R123` per una devolució. El número visible
normal del rebut continua sent `${ticket.printId()}`.

### Línies de producte

```xml
#foreach ($ticketline in $ticket.getLines())
<line>
    <text>${ticketline.printTicketName()}</text>
</line>
<line>
    <text align="left" length="25">${ticketline.printTicketReference()}</text>
    <text align="right" length="7">${ticketline.printPriceTaxAmount()}</text>
    <text align="right" length="3">${ticketline.printMultiply()}</text>
    <text align="right" length="7">${ticketline.printValueAmount()}</text>
</line>
#end
```

Altres valors disponibles habitualment:

```xml
${ticketline.printName()}
${ticketline.printPrice()}
${ticketline.printTax()}
${ticketline.printValue()}
${ticketline.printGiftVoucherCodes()}
```

Condicionar una línia a una propietat:

```xml
#if ($ticketline.hasGiftVoucherCodes())
<line>
    <text bold="true">Codi: ${ticketline.printGiftVoucherCodes()}</text>
</line>
#end
```

### Pagaments

```xml
#foreach ($paymentline in $ticket.payments)
<line>
    <text>${paymentline.name}</text>
    <text align="right">${paymentline.printTotal()}</text>
</line>
#end
```

El format per tipus de pagament es pot controlar amb `name`:

```xml
#foreach ($paymentline in $ticket.payments)
#if ($paymentline.name == "cash")
<line>
    <text>Efectiu</text>
</line>
<line>
    <text align="left" length="22">Entregat:</text>
    <text align="right" length="20">${paymentline.printPaid()}</text>
</line>
<line>
    <text align="left" length="22">Canvi:</text>
    <text align="right" length="20">${paymentline.printChange()}</text>
</line>
#end
#end
```

Tipus utilitzats per les plantilles actuals: `cash`, `cashrefund`, `magcard`,
`magcardrefund`, `cheque`, `chequerefund`, `paperin`, `paperout`, `free` i
`debt`.

## Codi de barres

El tag s’ha de posar dins de `ticket`, però no dins de `line`:

```xml
#if ($ticket.printId() != "")
<barcode type="CODE128" position="bottom">${ticket.printBarcodeId()}</barcode>
#end
```

`position="bottom"` imprimeix el valor llegible sota les barres. Amb la
plantilla actual es veurà, per exemple, `T123`.

No s’ha d’utilitzar `${ticket.printId()}` per a aquest codi si es vol que
l’escàner obri directament les opcions del tiquet. El prefix `T` identifica una
venda i `R` una devolució.

## Imatges i logotip

Les imatges també són recursos de la base de dades:

```xml
<ticket>
    <image>Printer.Ticket.Logo</image>
    <line>
        <text align="center">Botiga Exemple</text>
    </line>
</ticket>
```

El recurs `Printer.Ticket.Logo` ha de ser una imatge adequada per a impressió
monocromàtica ESC/POS. Els colors i les vores suaus poden perdre’s en una
impressora tèrmica.

## Calaix d’efectiu

Per obrir el calaix només en pagaments en efectiu:

```xml
#foreach ($paymentline in $ticket.payments)
#if ($paymentline.name == "cash" || $paymentline.name == "cashrefund")
<opendrawer/>
#end
#end
```

## Plantilla completa de referència

Aquest exemple combina capçalera, línies, total, pagaments i codi de barres:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<output>
    <display>
        <line>
            <text align="left" length="10">Total</text>
            <text align="right" length="10">${ticket.printTotal()}</text>
        </line>
    </display>

    <ticket>
        <image>Printer.Ticket.Logo</image>
        <line>
            <text align="center" length="42">Botiga Exemple</text>
        </line>
        <line>
            <text align="right" length="42">${ticket.printDateShort()}</text>
        </line>
        #if ($ticket.printId() != "")
        <barcode type="CODE128" position="bottom">${ticket.printBarcodeId()}</barcode>
        #end

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

        <line>
            <text align="left" length="22" bold="true">TOTAL</text>
            <text align="right" length="20" bold="true">${ticket.printTotal()}</text>
        </line>
        <line>
            <text align="center" length="42">Gràcies per la seva visita</text>
        </line>
    </ticket>

    #foreach ($paymentline in $ticket.payments)
    #if ($paymentline.name == "cash" || $paymentline.name == "cashrefund")
    <opendrawer/>
    #end
    #end
</output>
```

## Aplicar canvis a una instal·lació existent

1. Exporta o copia el contingut actual dels recursos abans d’editar-los.
2. Tanca el POS abans de modificar una base de dades Derby.
3. Edita el recurs des de **Administració → Recursos**. Els fitxers de
   `src-pos/com/openbravo/pos/templates/` són només la referència del format;
   substituir-los no actualitza una instal·lació existent.
4. Desa el recurs i reinicia el POS perquè la memòria cau de recursos es
   torni a carregar.
5. Imprimeix primer una previsualització i després un tiquet de prova.

## Problemes habituals

- **El canvi al fitxer XML no té efecte:** el POS utilitza la còpia del recurs
  desada a la base de dades, no el fitxer del projecte.
- **La previsualització és diferent del tiquet:** cal actualitzar també
  `Printer.TicketPreview`.
- **El tiquet regal no té codi:** cal actualitzar `Printer.TicketGift`.
- **S’imprimeix el text, però no les barres:** comprova que la impressora està
  configurada com a `epson` o `tmu220`, no com a `printer`; consulta
  `escpos-printer.md`.
- **El codi no s’obre com a tiquet:** comprova que el valor és `T<id>` o
  `R<id>` i que la versió del POS inclou `printBarcodeId()`.
- **El codi no surt en un tiquet nou:** l’ID encara pot ser buit abans de
  desar-se; la condició `#if ($ticket.printId() != "")` evita imprimir un codi
  invàlid.
