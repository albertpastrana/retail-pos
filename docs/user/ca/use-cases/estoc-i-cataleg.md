# Estoc i catàleg

**Situació:** Cal cercar productes, crear-ne un des d'un catàleg, demanar una reposició o registrar l'entrada de mercaderia.

## Cercar i crear un producte

1. Des de vendes, cerca pel nom, la referència/model, la categoria, la marca o la família.
2. Si un EAN és al catàleg compartit, el caixer pot obrir el diàleg de creació amb les dades del producte. Consulta [el flux de fallback](producte-no-trobat.md) per a les variants i l'estoc inicial.
3. Revisa el nom, la referència, la talla, el color, el preu i la categoria.
4. Desa només quan les dades siguin correctes.

Un producte venible és una fila per EAN. Les talles i els colors són variants del model, no atributs que facin triar una mida després d'escanejar.

## EAN desconegut

Si l'EAN no existeix ni a `PRODUCTS` ni al catàleg de fallback, vendes mostra un avís sota el teclat i no obre un diàleg de creació. Guarda el codi i passa'l a la persona d'estoc.

La cua compartida `PENDING_BARCODES` encara no està disponible. No assumeixis que un altre caixer pot veure aquest avís.

## Reposició i encàrrecs

1. Obre **Reposició i encàrrecs** i prem **Nova sol·licitud**.
2. Escaneja o introdueix l'EAN. Si no trobes el producte, cerca'l o introdueix una descripció manual.
3. Si és una petició d'una clienta, associa-hi la clienta i afegeix una nota si cal.
4. Desa la sol·licitud. No cal introduir una quantitat: cada entrada oberta representa que cal reposar aquell producte o variant.
5. Des de la llista, passa l'estat de **Pendent** a **Encarregat** quan s'hagi demanat al proveïdor i a **Rebut** quan arribi. Pots reobrir una entrada si l'encàrrec es cancel·la o no arriba.

Una entrada amb clienta és un encàrrec o reserva informativa. No crea una venda, no reserva estoc i no crea un tiquet. Les entrades obertes del mateix producte es reutilitzen per evitar duplicats.

## Entrada de mercaderia

L'entrada de mercaderia es registra des de **Estoc → Diari d'estoc**. Marcar una reposició com a **Rebuda** no actualitza l'estoc. Una comanda és una intenció de compra; una entrada al diari és el moviment que omple l'estoc.

## Imports i preus

Els fitxers TSV es preparen fora de l'aplicació. L'importador és una eina d'inicialització, no un backup. Les regles de preu poden proposar el preu de venda, però revisa sempre els casos excepcionals abans de desar.
