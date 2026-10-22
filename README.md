# Bloc-notes : éditeur de texte pour Android

|  |  |
|---|---|
| **Créateur** | Youssouf Diallo |
| **Projet** | Projet éditeur 2026, travail individuel |
| **Licence** | MIT (voir la section [Licence](#licence)) |
| **Dépôt** | [https://github.com/youmasther/BlocNote] |
| **Langage** | Java, librairies Android officielles |

## Description

Bloc-notes est un éditeur de texte pour appareil mobile Android, développé en **Java** avec **Android Studio** et les librairies officielles d'Android. L'application affiche la liste des notes de la plus récemment modifiée à la plus ancienne, permet de créer et de modifier des notes avec annulation et rétablissement des modifications, et enregistre automatiquement le travail de l'utilisateur.

L'interface suit les conventions visuelles d'Android : barre supérieure, bouton d'action flottant, menu latéral, cartes de notes et thème sombre.

## Fonctionnalités

### Exigences du mandat

| Exigence | État |
|---|---|
| Liste des notes triée de la plus récente à la plus ancienne | ✅ |
| Pour chaque note : nom, aperçu du contenu et date de dernière modification | ✅ |
| Création d'une nouvelle note depuis la liste | ✅ |
| Ouverture d'une note sélectionnée dans une page d'édition | ✅ |
| Modification du texte et enregistrement | ✅ |
| Annuler (undo) et refaire (redo) les modifications | ✅ |
| Récupération de la localisation | ✅ |

### Fonctionnalités supplémentaires

- **Sauvegarde automatique** : en plus du bouton d'enregistrement, la note est sauvegardée quand on quitte l'écran d'édition. Une note entièrement vide n'est jamais enregistrée.
- **Corbeille** : un appui long sur une note demande confirmation, puis l'envoie à la corbeille. On peut ensuite la **restaurer** ou la **supprimer définitivement**.
- **Menu latéral** (`DrawerLayout`) pour passer de « Mes notes » à « Corbeille ».
- **Lieu de prise de note** : à la première sauvegarde, l'application enregistre la position de l'appareil et le nom de la ville, affichés sur la carte de la note.
- **Thème sombre** avec une palette de couleurs personnalisée et des icônes SVG issues d'[Iconify](https://iconify.design), converties en `VectorDrawable`.

## Utilisation

1. **Créer une note** : appuyer sur le bouton « + » de la liste, écrire un titre et un texte.
2. **Enregistrer** : appuyer sur l'icône de sauvegarde, ou simplement revenir en arrière.
3. **Annuler / refaire** : utiliser les icônes undo et redo de la barre supérieure de l'écran d'édition.
4. **Modifier une note** : appuyer sur sa carte dans la liste.
5. **Supprimer une note** : appui long sur sa carte, puis confirmer.
6. **Retrouver ou vider la corbeille** : ouvrir le menu latéral (☰) et choisir « Corbeille ». Un appui long sur une note propose de la restaurer ou de la supprimer définitivement.

## Architecture

```
app/src/main/
├── java/com/example/blocnote/
│   ├── MainActivity.java       # Liste, menu latéral, corbeille, suppression
│   ├── EditionActivity.java    # Édition, undo/redo, sauvegarde, localisation
│   ├── Note.java               # Entité Room
│   ├── NoteDao.java            # Requêtes sur les notes
│   ├── AppDatabase.java        # Base Room (version 2) et migration 1 → 2
│   └── NoteAdapter.java        # Adapter du RecyclerView
└── res/
    ├── layout/                 # activity_main, activity_edition, item_note
    ├── menu/drawer_menu.xml    # Éléments du menu latéral
    ├── drawable/               # Icônes vectorielles, fond arrondi des cartes
    └── values/                 # colors.xml, themes.xml (+ values-night)
```

### Modèle de données

Table `notes` (Room) :

| Champ | Type | Rôle |
|---|---|---|
| `id` | int (auto-généré) | Clé primaire |
| `titre` | String | Nom de la note |
| `contenu` | String | Texte de la note |
| `dateModification` | long | Date de dernière modification (millisecondes) |
| `estSupprimee` | boolean | `true` quand la note est dans la corbeille |
| `latitude`, `longitude` | Double (nullable) | Position à la première sauvegarde |
| `localisation` | String (nullable) | Nom de la ville, ou coordonnées si le réseau est indisponible |

La corbeille est une **suppression logique** : la note reste dans la base avec `estSupprimee = true` jusqu'à sa suppression définitive.

### Choix techniques

- **Room** pour la persistance locale. Un changement de schéma passe par une migration, qui conserve les notes existantes.
- **Threads** : les accès à la base et le géocodage (réseau) s'exécutent sur un thread secondaire. `runOnUiThread` met ensuite l'interface à jour.
- **Cycle de vie** : la liste est rechargée dans `onResume()` et la note est sauvegardée dans `onPause()`.
- **Undo/redo** : un `TextWatcher` enregistre chaque état du texte dans deux piles (`Deque`). Annuler dépile une modification, la remet dans la pile de rétablissement, et toute nouvelle saisie vide cette dernière.
- **Localisation** : `LocationManager` (`Context.LOCATION_SERVICE`) fournit la position, et `Geocoder` la convertit en nom de ville.

## Compilation et exécution

### Prérequis

- **Android Studio** (version récente), qui inclut le JDK et le SDK Android nécessaires.
- Un **émulateur Android**, ou un téléphone avec le **débogage USB** activé.
- La version minimale d'Android prise en charge est celle définie par `minSdk` dans `app/build.gradle.kts`.

### Avec Android Studio

1. Cloner le dépôt :
   ```bash
   git clone https://github.com/youmasther/BlocNote.git
   ```
2. Ouvrir le dossier dans Android Studio (**File > Open**) et attendre la fin de la synchronisation Gradle.
3. Choisir un appareil dans la barre d'outils : créer un émulateur avec **Device Manager**, ou brancher un téléphone.
4. Lancer l'application avec le bouton ▶️.
5. Au premier lancement de l'écran d'édition, accepter la **permission de localisation**.

### En ligne de commande

Depuis la racine du projet :

```bash
./gradlew assembleDebug      # compile l'application
./gradlew installDebug       # compile et installe sur l'appareil connecté
```

L'APK de débogage se trouve dans `app/build/outputs/apk/debug/`.

### Localisation

- Sur un **téléphone**, la localisation doit être activée dans les réglages rapides, sinon aucune position n'est enregistrée.
- Sur un **émulateur**, la position par défaut est Mountain View (Californie). Elle se change dans *Extended controls (⋯) > Location*.

## Limites connues

- L'annulation et le rétablissement s'appliquent au **contenu** de la note, pas au titre.
- Le lieu n'est enregistré qu'à la **première sauvegarde** d'une note. Il n'est pas mis à jour ensuite.
- Sans permission ou sans localisation active, la note est enregistrée sans lieu.
- Si Room signale une incohérence de schéma après une modification de `Note`, il faut augmenter la version de la base et fournir une migration, ou effacer les données de l'application pendant le développement.

## Licence

Ce projet est distribué sous licence **MIT**. Le texte complet se trouve dans le fichier `LICENSE` à la racine du dépôt.

Copyright (c) 2026 [Prénom Nom]