TrueType builds of the same Manrope / IBM Plex Mono weights in `../fonts/` (which are `.woff2`, for the web design system), converted for Java's `Font.createFont` — see `../swing-development.md` § 2 for the registration gotcha (Manrope's internal family name is not "Manrope").

Regenerate if the source woff2 files ever change:

    pip install fonttools brotli
    python -m fontTools.ttLib.woff2 decompress -o Manrope-Regular.ttf ../fonts/Manrope-Regular.woff2
