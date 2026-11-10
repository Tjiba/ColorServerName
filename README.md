# ColorServerName

Mod Fabric client-side pour Minecraft 26.x. Colore le nom des serveurs dans la liste
Multijoueur via les codes `&` (`&4`, `&l`, `&r`…).

Un même JAR pour Minecraft 26.1, 26.1.1, 26.1.2, 26.2 et 26.3.
Les futures versions 26.x restent à vérifier à leur sortie.

Le rendu des `§` est déjà géré par vanilla ; seul le champ texte les refuse. Le mod
convertit `&` + `[0-9a-fk-or]` en `§` à la validation de l'écran Ajouter / Modifier un
serveur, et fait l'inverse à l'ouverture pour que le nom reste éditable.

- `&&` = un `&` littéral.
- Palette intégrée : 16 couleurs et 6 styles, avec leurs codes en infobulle.
- Aperçu du nom en direct sous la palette, compteur de caractères et interface FR/EN.
- Un clic insère le code au curseur ou l'applique à la sélection, puis rend le focus au nom.
- Le champ nom garde sa limite vanilla de 32 caractères, codes compris. Les noms déjà
  enregistrés restent préservés si l'échappement des `&` demande plus de place.
- Une insertion trop longue est refusée sans tronquer le nom.
- Aucun contournement du filtre de caractères : chat, enclume, livres et panneaux sont
  intouchés.
- Sans dépendance (pas de Fabric API), sans config, sans réseau.

## Build

```bash
./gradlew build
```

JAR commun `build/libs/colorservername-mc26.x-1.0.1.jar`. Java 25, Gradle 9.6,
Loom 1.17.11. Le build cible Minecraft 26.3 par défaut.

Pour compiler et tester contre une autre version sans changer le nom du JAR :

```bash
./gradlew build -Pminecraft_version=26.1
./gradlew build -Pminecraft_version=26.1.1
./gradlew build -Pminecraft_version=26.1.2
./gradlew build -Pminecraft_version=26.2
```

## Tests manuels

- Nom `&4Test` → rouge dans la liste, persiste après restart.
- `&&4Test` → affiche `&4Test` littéral.
- Réédition d'un serveur coloré → le champ montre `&4Test`, re-sauvegarde sans perte.
- Nom `§4Test` déjà présent dans `servers.dat` → rouge, réédition affiche `&4Test`.
- Sélectionner un mot puis cliquer sur une couleur / un style → aperçu mis à jour,
  sélection conservée et format du texte suivant inchangé.
- Sélectionner tout un nom déjà coloré → les couleurs de la sélection sont remplacées.
- Palette proche de la limite → message visible, aucun texte perdu.
- Redimensionner la fenêtre et changer l'échelle GUI → palette et boutons accessibles,
  nom en cours d'édition conservé.
- Avec Essential → Resource Packs et Show to Friends sur deux lignes distinctes,
  au-dessus de Done / Cancel, y compris en 320×240.
- Vérifier les libellés et infobulles en français et en anglais.
- `§` tapé en chat → toujours filtré, pas de kick.
