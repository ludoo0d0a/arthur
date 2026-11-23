# Roadmap — qualité visuelle et performance GenArt

**Statut : proposition, non implémentée ni mesurée.** Cette roadmap complète la [roadmap GenArt](../docs/roadmap-genart.md) et le [kit d'audit visuel](../docs/genart-ollama.md). Elle vise des œuvres procédurales plus immersives, animées durablement et adaptées aux capacités réelles du téléphone et de la TV, sans modifier l'identité du catalogue ni la sécurité d'Android Auto.

## 1. Point de départ

- Le module Android `:genart` (minSdk 26) utilise des moteurs procéduraux Compose Canvas, avec, pour certains packs, des shaders AGSL disponibles à partir d'Android 13/API 33 et un repli Canvas. Ce n'est pas un moteur 3D généraliste.
- [`GenartEffectCanvas`](../genart/src/main/kotlin/fr/geoking/arthur/genart/GenartEffectCanvas.kt) sélectionne l'échelle via [`GenartPerf`](../genart/src/main/kotlin/fr/geoking/arthur/genart/GenartPerf.kt), ajuste vitesse et luminosité selon `isActive`, et applique les finitions. `Low`, `Medium` et `High` contrôlent notamment l'échelle et certains flous ; tous les moteurs ne réduisent pas nécessairement leur nombre de primitives.
- La TV est actuellement sélectionnée en `Medium` dans [`AmbientScreen`](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/screens/AmbientScreen.kt) ; les transitions peuvent utiliser `Low` pour l'œuvre en arrière-plan dans [`ArtworkTransitionHost`](../androidApp/src/main/kotlin/fr/geoking/arthur/ui/components/ArtworkTransitionHost.kt). Ce sont des choix de qualité, pas une mesure de performance effective.
- [`GenartRegistry`](../genart/src/main/kotlin/fr/geoking/arthur/genart/GenartRegistry.kt) associe les moteurs aux rendus fixes et aux indicateurs de sécurité ; [`GenartStillRenderer`](../genart/src/main/kotlin/fr/geoking/arthur/genart/GenartStillRenderer.kt) sert notamment Auto et les aperçus. Certains rendus fixes utilisent un repli générique : la parité visuelle n'est donc pas acquise pour tous les moteurs.
- Les captures PNG et les vidéos issues d'une séquence d'images fixes du kit d'audit permettent de comparer l'aspect, **pas de mesurer les performances du rendu live**. Les performances, résolutions effectives et gains ci-dessous restent à établir sur appareils réels.

## 2. Résultat visé et garde-fous

Une image lisible de loin, des volumes et matériaux convaincants, des lumières douces, des mouvements organiques et une boucle discrète ; l'effet « wow » doit venir de la composition et de la profondeur, pas de flashs ni de flous systématiques. Respecter le caractère Ambient, la palette et l'identité de chaque œuvre.

- Préserver les identifiants `genart.*`, les packs, leurs droits, et les filtres `autoSafe` / `tvDreamSafe` existants. Aucun effet visuel ne rend automatiquement un moteur sûr pour Auto ou TV. Conserver les exceptions documentées, notamment Storm Strobe réservé au téléphone et les packs lourds exclus d'Auto.
- Auto reste un usage d'image fixe avec ses contraintes de sécurité : pas de scintillement, flash ou mouvement distrayant ; ne pas y activer de nouvelles simulations animées.
- Préserver la cohérence des rendus fixes (composition, palette, lumière et phase) et des animations ; lorsque le baker est un fallback, le signaler puis prévoir un rendu dédié pour les œuvres prioritaires.
- Proposer une adaptation automatique avec possibilité de réglage manuel si l'interface produit l'expose. Ne pas promettre 4K native ou 60 FPS sur tous les appareils.

### 4K : distinguer sortie et calcul

Une **sortie 4K UHD** correspond à 3 840 × 2 160 pixels (environ 8,3 Mpx). La **4K native** exige que l'image soit réellement calculée à cette résolution ; un rendu interne plus petit agrandi vers une sortie 4K n'est pas de la 4K native. Le scale appliqué au composable n'établit pas à lui seul la taille réelle d'un buffer GPU : confirmer la résolution par inspection/profilage du chemin effectif, y compris le mode de composition et les surfaces de la TV.

Cibler une sortie adaptée à la surface disponible et, lorsque cela apporte un gain visible, une résolution interne adaptative et un upscale propre. Privilégier les contours et détails importants sur les écrans 4K ; réserver la 4K native aux scènes et appareils dont le budget mesuré le permet. À titre d'ordre de grandeur, un buffer RGBA8 4K occupe environ 31,6 Mio, avant buffers supplémentaires, alignement, composition et autres ressources ; plusieurs passes peuvent multiplier ce coût.

## 3. Budgets et profils de départ

Ces valeurs sont des **objectifs initiaux à valider**, pas des caractéristiques garanties. Le budget total inclut application, rendu, composition et système. À 60 Hz, une échéance VSync représente 16,67 ms ; à 30 Hz, 33,33 ms. Ne pas confondre cadence d'écran, cadence de rendu cible et temps GPU disponible. Si un écran fonctionne à une autre fréquence, choisir une cadence soutenable compatible avec ses VSync et mesurer le résultat.

| Profil initial | Public / détection | Cible initiale | Politique visuelle |
|---|---|---|---|
| Économe | Téléphone ou TV limitée, mémoire ou état thermique contraint | 30 FPS réguliers | Résolution interne réduite, géométrie/particules plafonnées, lumière analytique, pas de passes coûteuses |
| Équilibré | Téléphone courant, TV courante ; point de départ prudent | 60 FPS si soutenus, sinon 30 FPS réguliers | Détails moyens, ombres et reflets simplifiés, upscale adaptatif |
| Élevé | Appareil validé par mesures, alimentation et température favorables | 60 FPS soutenus | Effets choisis par scène, résolution supérieure ; 4K native uniquement si mesurée soutenable |

La catégorie « TV 4K » ne présume pas de la puissance GPU ni de la taille effective de la surface. Initialiser à partir du type d'appareil, de la résolution réellement disponible, du niveau API et des capacités accessibles ; affiner par les mesures live et l'état thermique plutôt que par une liste figée de modèles. Les profils doivent plafonner séparément résolution, primitives, simulations et passes ; la résolution seule ne corrige pas un goulot CPU.

## 4. Chantiers visuels et algorithmiques

| Priorité | Chantier | Approche proposée | Garde-fou / validation |
|---|---|---|---|
| P0 | Direction artistique | Sélectionner quelques œuvres représentatives (eau, géométrie, particules, ambiance) ; définir cadrage, palette, hiérarchie des détails et références avant/après. | Revue sur téléphone et TV à distance de visionnage ; pas de filtre uniforme imposé au catalogue. |
| P1 | Lumière et ombres | Dégradés orientés, masques et occlusion simplifiée ; halo pré-calculé ou flou réduit, ombre de contact et lumière rasante selon la scène. | Limiter surfaces hors écran et surdessin ; garder une variante sans flou ; pas de flash sur les surfaces Ambient sûres. |
| P1 | Profondeur / formes 3D | Pseudo-3D par projection, parallaxe lente, couches, normales ou dégradés analytiques ; géométrie limitée et stable. Une vraie 3D n'est envisagée qu'après prototype comparatif. | Pas de dépendance à un nouveau moteur 3D par défaut ; limiter allocations et triangles ; vérifier lisibilité et absence d'aliasing en mouvement. |
| P1 | Matériaux, reflets | Brillance directionnelle, Fresnel approximé, reflets procéduraux ou par miroir et masque, rugosité par couches simples ; réutiliser les informations de la scène. | Pas de réflexion écran-espace ni ray tracing par défaut ; désactiver/réduire les passes de réflexion selon le budget. |
| P1 | Mouvement et physique légère | Ondes analytiques, vent partagé, particules bornées, amortissement et Verlet court si utile ; temps et phase explicites. | Coût borné par profil, pas d'allocations par particule et par frame ; boucle continue sans saut de position ni de vitesse perceptible. |
| P2 | Simulations / shaders lourds | Expérimenter les champs de réaction et Physarum dans leurs packs dédiés ; AGSL API 33+ avec variante Canvas. | Préserver les exclusions Auto, plafonner mémoire/résolution/fréquence de simulation ; vérifier le fallback et ne pas bloquer la première image. |
| P2 | Finition 4K | Anticrénelage adapté, détails procéduraux stables et upscale comparé à la référence native. | Éviter scintillement, moiré et netteté artificielle ; juger sur sortie réelle, pas seulement sur une capture agrandie. |

### Exemples de traitements par famille

- **Eau et météo** : ondes à plusieurs échelles, normales approximées, lumière rasante et reflet déformé ; écume ou brume limitées et supprimables indépendamment.
- **Géométrie et cristaux** : facettes éclairées analytiquement, ombres de contact et liseré discret ; limiter le nombre de facettes et les couches transparentes.
- **Particules et ciel** : profondeur par taille, vitesse et opacité, halos pré-calculés et densité plafonnée ; éviter les changements brusques de densité.
- **Intérieurs et nature** : plans en parallaxe lente, lumière masquée et ombres simplifiées ; un rendu fixe cohérent pour Auto et la grille.

## 5. Animation adaptative et optimisation

1. **Mesurer avant de modifier** : repérer séparément recompositions, travail CPU, dessin, GPU, allocations, mémoire et composition. Mettre en cache géométrie, gradients, chemins et ressources stables lorsque leur invalidation est maîtrisée ; déplacer la lecture du temps vers la phase de dessin lorsque cela réduit réellement le travail Compose.
2. **Maîtriser le cycle de vie** : éviter de calculer une œuvre hors écran ou sans surface visible ; suspendre ou réduire explicitement les animations selon le cycle de vie. Aujourd'hui `isActive` réduit vitesse et intensité : ne pas le considérer comme une suspension complète. Préserver phase et reprise sans saut visible.
3. **Gérer le temps** : piloter par une horloge monotone, phase indépendante du nombre d'images, pas de simulation borné et limite de rattrapage après pause ; interpoler si nécessaire. Éviter les mises à jour simultanées inutiles dans les transitions à deux œuvres.
4. **Dégrader progressivement** : d'abord réduire fréquence de simulation, détails et effets secondaires, puis résolution interne et enfin cadence cible si nécessaire ; prioriser la lisibilité de l'œuvre. Mesurer après chaque étape car l'ordre optimal dépend du goulot CPU/GPU. Toujours conserver une variante de base sûre et cohérente.
5. **Éviter les oscillations** : fenêtre glissante de temps de frame, seuils distincts de montée/descente et délai minimal entre changements ; rétablissement plus lent que la dégradation. Réagir rapidement à une pression thermique/mémoire signalée par la plateforme, sans collecter d'identifiants personnels.
6. **Réduire le coût graphique** : limiter surdessin, surfaces intermédiaires, flous plein écran et changements de shader ; mutualiser les calculs et plafonner les buffers. Vérifier sur GPU réels : une optimisation CPU peut déplacer le coût vers le GPU et inversement.

Paramètres, seuils d'hystérésis et cadence de reprise restent à calibrer à partir de la phase de mesure. Ne pas changer brusquement de palette ou de composition lors d'une adaptation.

## 6. Phases et critères de sortie

### P0 — Référence et instrumentation

- Constituer une matrice d'appareils : au moins un téléphone modeste et un performant, une TV modeste et une TV 4K, avec API/GPU et résolution effective consignés. Ajouter un appareil API < 33 pour le repli Canvas.
- Mesurer à froid puis après stabilisation : démarrage jusqu'à la première image, temps de frame p50/p95/p99, proportion de frames hors délai, cadence effective, allocations, mémoire et état thermique ; distinguer CPU, GPU et composition lorsque les outils le permettent. Enregistrer la fréquence écran, la qualité, la résolution interne observée, l'œuvre et le scénario.
- Scénarios : aperçu seul, transition entre deux œuvres, retour arrière-plan/premier plan, rotation d'œuvres et session prolongée. Utiliser des builds et conditions comparables, outils Android de profilage/traces et Macrobenchmark ou instrumentation équivalente selon faisabilité du projet.
- **Sortie** : tableau de référence reproductible et liste des principaux goulots pour chaque profil ; aucune promesse de gain avant mesure.

### P1 — Socle adaptatif

- Introduire un budget par profil au point d'entrée et dans les moteurs pilotes : nombre maximal de primitives, résolution, effets et fréquence de simulation ; garder les paramètres de qualité existants compatibles.
- Ajouter les adaptations pilotées par mesure, l'hystérésis et le cycle de vie ; tester la transition et le retour d'une application suspendue. Fournir une méthode de diagnostic local indiquant profil, résolution, cadence et raison de dégradation.
- **Sortie** : sur la matrice, viser p95 sous l'échéance de la cadence retenue (16,67 ms à 60 FPS ou 33,33 ms à 30 FPS), sans oscillation visible entre profils et sans régression de plantage ou mémoire par rapport à P0. Documenter les écarts et sélectionner 30 FPS si 60 FPS n'est pas soutenable.

### P2 — Pilotes visuels et parité fixe

- Améliorer un moteur représentatif de chaque famille retenue avec ses variantes de qualité et un rendu fixe dédié ou corrigé ; comparer avant/après à paramètres, phase et palette identiques.
- Vérifier boucles, contours, ombres, reflets, composition et stabilité en mouvement ; comparer API < 33 et API 33+ lorsque le pilote utilise AGSL. Appliquer les restrictions catalogue et sécurité existantes.
- **Sortie** : validation artistique avant/après sur téléphone et TV ; aucune rupture perceptible en fin de boucle ; pas de régression des budgets P1 ; rendu fixe reconnaissable et cohérent avec le live. Tout écart lié à un fallback restant est consigné.

### P3 — Extension et robustesse

- Étendre par pack selon impact visuel et coût mesuré ; préserver les identifiants, la distribution et les tests de catalogue. Garder les simulations lourdes dans leurs packs autorisés.
- Faire tourner au moins 30 minutes sur téléphone et 2 heures en TV, avec transitions, reprise et changement d'état thermique si disponible. Documenter conditions d'alimentation et température ambiante ; surveiller dérive mémoire, throttling, crashes et stabilité des temps de frame.
- **Sortie** : pas de croissance mémoire non bornée, aucune fuite ou crash observé ; cadence cible et p95 tenus après stabilisation sur les appareils de référence, ou profil/cadence adaptés et écarts explicitement documentés. Suivre aussi p99 et frames hors délai pour détecter les à-coups isolés ; fixer leurs seuils de livraison après P0.

### P4 — 4K et effets avancés conditionnels

- Comparer sortie 4K native et rendus internes agrandis, à scène et conditions identiques. Tester reflets/lumières avancés uniquement sur matériel compatible ; mesurer coût supplémentaire, mémoire et avantage visuel.
- **Sortie** : activer une variante 4K native ou un effet avancé seulement si la différence est perceptible et que les critères P1/P3 restent tenus ; sinon conserver l'upscale ou le fallback documenté. Aucun moteur graphique ou effet coûteux n'est imposé à tout le catalogue.

## 7. Protocole de validation et livraison

- Pour chaque pilote, archiver appareil, version Android, fréquence d'affichage, scène, graine, phase, qualité, résolution de surface et résolution interne observée, durée, alimentation, métriques et captures. Comparer à la référence P0 dans les mêmes conditions.
- Exploiter les captures existantes : `./gradlew :androidApp:generateGenartScreenshots` et `./gradlew :androidApp:generateGenartPreviewFrames` ; voir le [kit](../docs/genart-ollama.md) pour les paramètres et comparaisons PNG/MP4. Ces images fixes et séquences de phases complètent, mais ne remplacent pas, la mesure du chemin live sur matériel réel.
- Tester les chemins Canvas et AGSL applicables, les différents niveaux de qualité, l'absence de surface visible, la reprise, les transitions à deux œuvres et les rendus fixes/fallbacks. Exécuter les tests de catalogue et les tests concernés par les changements, puis vérifier manuellement les restrictions Auto/TV.
- À chaque livraison : revue visuelle, tableau des mesures avant/après et écarts, tests, identification des appareils non couverts et possibilité de revenir au profil ou moteur précédent. Refuser une amélioration visuelle qui détruit la fluidité soutenue ou la sécurité d'un usage existant.

## 8. Décisions ouvertes

- Fixer les seuils p99 et de frames hors délai après collecte P0, par cadence et type d'appareil.
- Choisir les premières œuvres pilotes selon les captures et les coûts réellement observés ; déterminer pour chacune si le rendu fixe mérite un baker dédié.
- Décider après prototypage si une abstraction de rendu supplémentaire ou un backend 3D apporte assez de bénéfice pour justifier maintenance, mémoire et compatibilité. Aucune migration de moteur n'est un prérequis de cette roadmap.
