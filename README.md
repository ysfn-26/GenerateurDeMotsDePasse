# Outil DevOps de Gestion et d'Audit de Mots de Passe 🔐

Application modulaire de génération et d'audit de la force des mots de passe développée en **Java 21** et conteneurisée avec **Docker**.

Ce projet met en œuvre une architecture découplée où l'application cliente interagit en ligne de commande (CLI) avec l'utilisateur, tandis que l'évaluation de la robustesse est déléguée à un micro-service isolé (serveur TCP) fonctionnant dans un conteneur Linux Alpine équipé de la bibliothèque de sécurité **CrackLib**.

## 🚀 Fonctionnalités

- **Générateur de mots de passe cryptographiquement sécurisés** : Personnalisation de la longueur (4 à 64 caractères) et des types de caractères (Majuscules, Minuscules, Chiffres, Symboles).
- **Mode Rafale** : Génération simultanée de plusieurs mots de passe (jusqu'à 50 d'un coup).
- **Audit de sécurité distant via Sockets TCP** : Communication réseau bidirectionnelle standardisée sur le port `12345`.
- **Analyse par CrackLib** : Vérification de la robustesse basée sur les dictionnaires de sécurité natifs de Linux.
- **Haute résilience (Mode Déconnecté)** : Gestion transparente des pannes réseau via un mécanisme de repli affichant `Docker KO` si le conteneur est arrêté ou inaccessible.

## 📁 Structure du Projet

```text
DevOps-Password-Tool/
├── .gitignore               # Exclusion des fichiers compilés, du cache macOS et d'IntelliJ
├── docker/                  # Composant Infrastructure (Serveur d'audit)
│   ├── AuditServer.java     # Code source du serveur TCP d'écoute réseau
│   └── Dockerfile           # Recette de l'image Docker (Alpine + CrackLib)
└── src/                     # Composant Applicatif (Client)
    └── PasswordApp.java     # Code source de l'interface utilisateur et générateur
