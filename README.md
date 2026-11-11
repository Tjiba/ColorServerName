# ColorServerName

A small client-side Fabric mod to color and format server names in your multiplayer list.

Palette and style buttons insert codes such as `&c` and `&l` at the cursor, or format selected text. A live preview shows the result.

- 16 colors and formatting controls.
- Formatting codes stay visible. Only per-character gradient colors stay hidden; use `&l` before the text for a bold gradient.
- Custom HEX colors and two-color gradients: click `+` next to Reset.
- Minecraft 26.2 and 26.3 only.
- Requires Fabric Loader 0.19.3+ and Java 25+.
- No Fabric API or Fabric Language Kotlin needed.

Drop the JAR into your `mods` folder, then add or edit a server.

Build from source: `./gradlew build`.
