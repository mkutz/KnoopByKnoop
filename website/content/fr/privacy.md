---
translates: 2026-11-27
---
# Politique de confidentialité

Dernière mise à jour : 27 novembre 2026

Cette politique s'applique aux applications Knoop by Knoop pour Android et iOS et au site [knoopbyknoop.com](https://knoopbyknoop.com).


## En bref

Knoop by Knoop n'a ni comptes, ni publicité, ni analyse d'audience, ni rapports de plantage.
Votre position et vos itinéraires restent sur votre appareil.
L'application ne communique qu'avec trois services : pour télécharger les données des réseaux, afficher le fond de carte et rechercher des lieux.
Quand vous planifiez un itinéraire ou touchez un lieu pendant la planification, GitHub peut aussi en voir la zone approximative, car les chemins et les détails des lieux alentour y sont téléchargés, mais jamais l'endroit où vous êtes.


## Responsable

Michael Kutz\
Wellenstr. 52, 53721 Siegburg\
mail@knoopbyknoop.com


## Données qui restent sur votre appareil

- **Position.** Pendant la navigation, l'application utilise votre position pour vous annoncer le prochain point-nœud. Elle n'est utilisée que sur l'appareil et n'est jamais envoyée. L'application n'accède à votre position que pendant son utilisation, ou pendant une navigation, signalée par une notification.
- **Itinéraires et réglages.** Votre itinéraire actuel, les réseaux, chemins et détails des lieux téléchargés et vos réglages sont enregistrés sur l'appareil. Ils sont supprimés quand vous désinstallez l'application.
- **Itinéraires partagés.** Quand vous partagez un itinéraire, le fichier (un itinéraire pour Knoop by Knoop ou un fichier GPX) contient les points-nœuds de l'itinéraire et, pour le GPX, sa trace. Il ne va qu'aux personnes avec qui vous le partagez, par l'application que vous choisissez.


## Services contactés par l'application

Toutes les connexions sont chiffrées (HTTPS).
Comme toute connexion à Internet, chaque requête révèle au service votre adresse IP et des informations de base sur l'appareil (comme l'agent utilisateur de l'application).
L'application n'envoie aucun identifiant propre.

| Service | Finalité | Ce qu'il reçoit |
|---|---|---|
| [GitHub](https://github.com) (GitHub, Inc., États-Unis) | Téléchargement des réseaux de points-nœuds, au premier lancement et au plus une fois par jour, des chemins autour d'un itinéraire vélo planifié, et des détails des lieux autour d'un itinéraire planifié ou d'un lieu touché pendant la planification | Adresse IP, la zone approximative des itinéraires planifiés et des lieux touchés pendant la planification |
| [OpenFreeMap](https://openfreemap.org) | Tuiles du fond de carte pendant l'affichage de la carte | Adresse IP, la zone de carte affichée |
| [Photon](https://photon.komoot.io) de komoot (komoot GmbH, Allemagne) | Recherche de lieux | Adresse IP, le texte saisi dans le champ de recherche, le centre de la zone du réseau |

**Guidage hors de l'itinéraire.** L'application télécharge depuis GitHub les chemins situés à environ 5 km d'un itinéraire vélo que vous avez planifié, par carrés d'environ 7 sur 4 km. Les carrés demandés montrent à GitHub, avec votre adresse IP, la zone approximative de cet itinéraire, et peuvent être téléchargés pendant la navigation s'ils n'étaient pas complets auparavant. Ils sont choisis uniquement d'après l'itinéraire : votre position n'est jamais utilisée pour les choisir et n'est jamais envoyée. Le chemin de retour vers l'itinéraire est calculé sur votre appareil. Vous pouvez supprimer les chemins téléchargés dans la fiche d'informations ; ils seront téléchargés à nouveau pour le prochain itinéraire planifié.

**Détails des lieux.** Les sites web, numéros de téléphone et horaires d'ouverture des lieux sur la carte sont téléchargés depuis GitHub dans les mêmes carrés : ceux situés à environ 5 km d'un itinéraire planifié, à vélo ou à pied, et le carré d'un lieu que vous touchez pendant la planification, s'il n'est pas encore sur l'appareil. Cela montre à GitHub, avec votre adresse IP, la zone approximative de l'itinéraire ou du lieu. Pendant la navigation, toucher un lieu ne télécharge jamais rien. Ouvrir le site d'un lieu ou appeler son numéro se fait dans votre navigateur ou votre application de téléphone, pas dans Knoop by Knoop. Vous pouvez supprimer les détails des lieux téléchargés dans la fiche d'informations.

Ces services traitent les données selon leurs propres politiques de confidentialité :
[GitHub](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement),
[OpenFreeMap](https://openfreemap.org/privacy/),
[komoot](https://www.komoot.com/privacy).

La base légale est l'art. 6, paragraphe 1, point b), du RGPD (fournir les fonctions que vous utilisez) et l'art. 6, paragraphe 1, point f), du RGPD (notre intérêt légitime à faire fonctionner l'application).


## Autorisations

- **Position** pour naviguer. La planification d'itinéraires fonctionne sans.
- **Notifications** pour afficher le prochain point-nœud sur l'écran verrouillé pendant la navigation.
- **Internet** pour les services ci-dessus.


## Ce site

Le site knoopbyknoop.com est hébergé par [GitHub Pages](https://pages.github.com) (GitHub, Inc., États-Unis).
Quand vous ouvrez une page, GitHub reçoit votre adresse IP et ce que votre navigateur envoie avec la requête, comme la page, l'heure et l'agent utilisateur du navigateur, et peut les conserver dans ses journaux pour des raisons de sécurité, selon [la déclaration de confidentialité de GitHub](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement).
Le site ne dépose aucun cookie, n'utilise ni analyse d'audience ni pistage, et ne charge rien depuis d'autres sites.
La base légale est l'art. 6, paragraphe 1, point f), du RGPD (notre intérêt légitime à afficher le site de manière sûre).


## Vos droits

Comme nous ne conservons aucune donnée personnelle vous concernant, il n'y a généralement rien à consulter, rectifier ou supprimer.
Pour les données traitées par les services ci-dessus, adressez-vous directement à eux.
Vous avez le droit d'introduire une réclamation auprès d'une autorité de contrôle de la protection des données.


## Modifications

Les modifications de cette politique sont publiées ici et suivies dans l'[historique du dépôt](https://github.com/mkutz/knoopbyknoop/commits/main/PRIVACY.md).
