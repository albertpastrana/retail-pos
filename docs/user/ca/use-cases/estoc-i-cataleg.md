# Estoc i catàleg

**Situació:** Cal cercar productes, crear-ne un des d'un catàleg o registrar l'entrada de mercaderia.

## Cercar i crear un producte

1. Des de vendes, cerca pel nom, la referència/model, la categoria, la marca o la família.
2. Si un EAN és al catàleg compartit, el caixer pot obrir el diàleg de creació amb les dades del producte.
3. Revisa el nom, la referència, la talla, el color, el preu i la categoria.
4. Desa només quan les dades siguin correctes.

Un producte venible és una fila per EAN. Les talles i els colors són variants del model, no atributs que facin triar una mida després d'escanejar.

## EAN desconegut

Si l'EAN no existeix ni a `PRODUCTS` ni al catàleg de fallback, vendes mostra un avís sota el teclat i no obre un diàleg de creació. Guarda el codi i passa'l a la persona d'estoc.

La cua compartida `PENDING_BARCODES` encara no està disponible. No assumeixis que un altre caixer pot veure aquest avís.

## Entrada de mercaderia

L'entrada de mercaderia es registra des de **Estoc → Diari d'estoc**. Importar o crear un producte no crea unitats d'estoc. Una comanda és una intenció de compra; una entrada al diari és el moviment que omple l'estoc.

## Imports i preus

Els fitxers TSV es preparen fora de l'aplicació. L'importador és una eina d'inicialització, no un backup. Les regles de preu poden proposar el preu de venda, però revisa sempre els casos excepcionals abans de desar.
