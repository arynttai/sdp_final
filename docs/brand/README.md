# ITALIANO identity

The current site uses two user-supplied transparent wordmarks:

- `../../src/main/resources/static/images/italiano-logo-light.png`: red lettering for the light theme.
- `../../src/main/resources/static/images/italiano-logo-dark.png`: cream lettering for the dark theme.

Both original 2172 x 724 PNG files are stored unchanged. Header and footer layout
crops their transparent margins through CSS. The dark image includes its original
soft halo. `italiano-icon.svg` is a small pizza-slice browser icon in the same colors.
The site name and all interface copy are English. Theme selection is stored under
`italiano-theme`; switching themes preserves cart and checkout data.

## Previous Aituria emblem (archive)

The November 14, 2026 emblem replaces the earlier serif A and flame. A substantial
folded bronze ribbon forms the A, with an arch-shaped opening suggesting an oven.
The master has a transparent background and was checked against dark espresso and
warm ivory backgrounds.

Created using OpenAI's built-in image generator. Higgsfield rejected the request
because the selected account required Basic or higher; the user approved using
the available generator instead. No Higgsfield-generated logo is claimed.

## Assets

- `aituria-emblem-master.png`: original 1254 x 1254 RGBA artwork.
- `../../src/main/resources/static/images/aituria-emblem.png`: 256 x 256 website asset.
- `../../src/main/resources/static/images/favicon.png`: 64 x 64 browser icon.
- `../../src/main/resources/static/images/apple-touch-icon.png`: 180 x 180 touch icon.

Website derivatives preserve the original alpha channel and use high-quality
resampling. The emblem appears in the header and footer beside accessible live text.

## Verification

`mvnw.cmd clean verify` passed with JDK 21: 61 tests, no failures, errors, or skips.
Chromium checks at 1440px and 390px confirmed the header emblem loads at 48px and
38px respectively, with no horizontal overflow or JavaScript/resource errors.
These results describe the previous brand implementation. Current screenshots
in `docs/screenshots/` show ITALIANO instead.

## Generation prompt

Use case: logo-brand. Create one exceptional, production-ready sculptural 3D emblem
for AITURIA, a contemporary pizza and pasta restaurant with a dark espresso and warm
ivory website. This is a brand new logo, not an edit of the rejected serif A/flame.
Deliver a single isolated symbol on a genuinely transparent background, no text,
mockup, board, or extra icons. Design a bespoke uppercase A as one elegantly folded,
substantial champagne-bronze metal ribbon. The arch-shaped negative space should
subtly suggest a wood-fired oven. Rounded triangular silhouette, confident broad
curves and precisely machined edges. An asymmetric ribbon crossover creates a
distinctive custom monogram, instantly recognizable as an A at 32 pixels.
Contemporary architectural Italian restaurant identity. Satin-brushed pale bronze
front surfaces, deep warm copper sidewalls, fine polished bevels, physically
plausible reflections and subtle microtexture. Nearly frontal with a slight
three-quarter view to reveal generous extrusion thickness. Soft studio key from
upper left, restrained rim highlights. Crisp clean silhouette and transparent
alpha. No colorful pixels, flame, stars, wreath, crown, thin serif font, marble,
sparkles, floor, floating shadow, background color, or checkerboard. Centered square
composition, mark occupies 84 percent of canvas. Careful geometric proportions,
high-quality photorealistic 3D product render with readable shadowed sides.
