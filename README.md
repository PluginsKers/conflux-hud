# Conflux HUD

A compact, vanilla-style HUD for **Eyreland's Conflux server** ([`eyreland:conflux`](https://gitea.i5res.com/eyreland/conflux)). This is a companion to that server plugin; other servers and similarly named projects are unsupported.

The HUD sits flush against the top center of the screen, with a vanilla button background and rounded lower corners. It shows:

- Own, peaceful and hostile territories, unit counts, flag proximity and attack warnings.
- Landholders and selected land deed points.
- Wandering trader shopping estimates and the last change in buy and sell amounts, in emeralds.

Installation is optional for each player. Players without the mod keep Conflux's original interface. The mod reads the plugin's existing BossBar, TAB footer, action bar and item data; it does not change transactions, permissions or server gameplay. No BetterHud installation or resource pack is needed.

TAB and ordinary BossBars appear below the HUD. F1 hides it. Labels follow the game's English or Simplified Chinese setting; names and server messages keep their original language.

## Installation

Requires Fabric Loader 0.19.5 or newer and Fabric API for the selected Minecraft version.

| Minecraft | Java |
| --- | --- |
| 1.21.11 | 21 |
| 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 25 |

Download the matching jar from the [build workflow's artifacts](https://github.com/PluginsKers/conflux-hud/actions/workflows/build.yml) and place it in your client's `mods` folder. Install only one Conflux HUD jar. Each build targets its exact Minecraft version; it does not provide cross-version server connectivity.

## Server data

The supported formats are those used by Eyreland's Conflux v2.0.20. Recognition uses text and item tags, not an authenticated server handshake. A different server sending identical data can also trigger the HUD; modified Conflux messages may not match.

Shopping amounts are estimates until the server settles the session on closing the cargo barrel. “Last change” is the difference between consecutive server quotes, including negative amounts when undoing a purchase or sale. Rapid actions can be combined into one quote, so the mod cannot separate those actions or infer individual item prices. Invalid quotes clear the previous amounts; closing the container or disconnecting clears the session.

## Building

```sh
./gradlew build compileGametestJava
./gradlew -Pminecraft_version=1.21.11 build compileGametestJava
./gradlew -Pminecraft_version=1.21.11 runClientGameTest
```

Use `gradlew.bat` on Windows. The default target is 26.3; `minecraft_version` accepts the versions listed above. Gradle resolves the required Java toolchain. Client game tests require a display.

Jars are written to `build/<minecraft_version>/libs/`. Install the jar without the `-sources` suffix. GitHub Actions builds and runs unit tests for every supported version on pull requests and pushes to `main`.

## License

[MIT](LICENSE).
