# Dos caixers i la LAN de la botiga

**Situació:** La botiga té més d'un caixer i tots han de treballar sobre les mateixes dades.

## Abans d'obrir els caixers

1. Engega el Linux que allotja PostgreSQL.
2. Comprova que PostgreSQL està en funcionament abans d'obrir els altres caixers.
3. Cada caixer ha d'utilitzar el mateix `db.URL` del servidor PostgreSQL.
4. La impressora, la pantalla de client, l'idioma i la configuració local són propis de cada caixer.

No utilitzis Derby compartit, Syncthing ni git per sincronitzar dades. Els tiquets, el catàleg, l'estoc i les caixes pertanyen a la base de dades compartida.

## Treballar amb dos caixers

- Una línia escanejada al caixer satèl·lit apareix al caixer central sense haver d'aparcar el tiquet.
- El tiquet obert es pot reclamar des de l'altre caixer; el bloqueig evita edicions antigues.
- Només el caixer amb impressora pot cobrar i imprimir el tiquet.
- El PostgreSQL central és la font de veritat per als productes, l'estoc i els tiquets.

## Si el Linux està apagat

Els altres caixers no poden vendre. No canviïs a una còpia Derby ni intentis recuperar la venda amb una base de dades local. Torna a engegar PostgreSQL i mostra l'error a la persona responsable si la connexió no es recupera.

## Backup

Des del caixer Linux, utilitza **Manteniment** per crear el backup. Copia el fitxer resultant a un USB o a un altre equip segons el procediment de la botiga. El backup no substitueix la base de dades compartida i el fitxer TSV del catàleg no és un backup.

No copiïs contrasenyes al repositori ni comparteixis el fitxer de configuració amb credencials.
