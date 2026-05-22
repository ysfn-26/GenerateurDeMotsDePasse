# Image de base Alpine avec le JDK 21 — légère et suffisante pour compiler et exécuter Java
FROM eclipse-temurin:21-jdk-alpine

# Installation de CrackLib via le gestionnaire de paquets Alpine (apk)
# cracklib-check est le binaire CLI qui analyse les mots de passe
# cracklib-words installe le dictionnaire de mots courants utilisé par CrackLib
RUN apk add --no-cache cracklib cracklib-words

# Répertoire de travail dans le conteneur
WORKDIR /app

# Copie du source Java du serveur d'audit dans le conteneur
COPY AuditServer.java .

# Compilation du serveur d'audit directement dans le conteneur
# Le .class produit reste dans /app
RUN javac AuditServer.java

# Déclaration du port exposé (documentaire — le -p du docker run reste nécessaire)
EXPOSE 12345

# Démarrage du serveur d'audit au lancement du conteneur
CMD ["java", "AuditServer"]