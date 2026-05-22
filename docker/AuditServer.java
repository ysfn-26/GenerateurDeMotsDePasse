import java.io.*;
import java.net.*;

/**
 * Serveur TCP d'audit de mots de passe.
 * Écoute sur le port 12345, reçoit un mot de passe,
 * le soumet à CrackLib via la commande système "cracklib-check",
 * puis renvoie un score textuel au client Java.
 */
public class AuditServer {

    private static final int PORT = 12345;

    public static void main(String[] args) throws IOException {

        // Création du ServerSocket — écoute sur toutes les interfaces du conteneur
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("[AUDIT] Serveur démarré sur le port " + PORT);

            // Boucle infinie : on traite une connexion, puis on recommence
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("[AUDIT] Connexion reçue de : " + client.getInetAddress());

                // Chaque connexion est traitée dans un thread séparé
                // pour ne pas bloquer les connexions suivantes (mode rafale)
                new Thread(() -> handleClient(client)).start();
            }
        }
    }

    /**
     * Gère une connexion cliente : lit le mot de passe,
     * interroge CrackLib, renvoie le score.
     */
    private static void handleClient(Socket client) {
        try (
                BufferedReader in  = new BufferedReader(
                        new InputStreamReader(client.getInputStream(),  "UTF-8"));
                PrintWriter    out = new PrintWriter(
                        new OutputStreamWriter(client.getOutputStream(), "UTF-8"), true)
        ) {
            // Lecture du mot de passe envoyé par PasswordApp.java
            String password = in.readLine();
            System.out.println("[AUDIT] Mot de passe reçu : " + password);

            // Appel à CrackLib et conversion du résultat en score lisible
            String score = evaluateWithCrackLib(password);
            System.out.println("[AUDIT] Score renvoyé : " + score);

            // Envoi du score au client
            out.println(score);

        } catch (IOException e) {
            System.err.println("[AUDIT] Erreur client : " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Appelle "cracklib-check" en ligne de commande via ProcessBuilder.
     * cracklib-check lit un mot de passe sur stdin et répond sur stdout
     * sous la forme : "monMotDePasse: OK" ou "monMotDePasse: it is too short"
     * On parse cette réponse pour produire un score sur 5 niveaux.
     */
    private static String evaluateWithCrackLib(String password) throws IOException {

        // Lance la commande cracklib-check installée dans le conteneur Alpine
        ProcessBuilder pb = new ProcessBuilder("cracklib-check");
        pb.redirectErrorStream(true); // fusionne stderr dans stdout pour simplifier la lecture
        Process process = pb.start();

        // Envoie le mot de passe sur stdin du processus cracklib-check
        try (PrintWriter stdin = new PrintWriter(
                new OutputStreamWriter(process.getOutputStream(), "UTF-8"), true)) {
            stdin.println(password);
        }

        // Lit la réponse complète de cracklib-check sur stdout
        String cracklibOutput;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            cracklibOutput = reader.readLine(); // format : "password: OK" ou "password: raison"
        }

        System.out.println("[AUDIT] Réponse CrackLib brute : " + cracklibOutput);

        // Conversion de la réponse CrackLib en score sur 5 niveaux
        return convertToScore(password, cracklibOutput);
    }

    /**
     * Convertit la réponse brute de CrackLib + la longueur du mot de passe
     * en l'un des 5 scores exigés par le cahier des charges.
     *
     * Logique de scoring :
     *   - CrackLib dit "OK" ET longueur >= 16  → Très fort
     *   - CrackLib dit "OK" ET longueur >= 12  → Fort
     *   - CrackLib dit "OK" ET longueur >= 8   → Moyen
     *   - CrackLib rejette (trop court, dictionnaire, etc.) ET longueur >= 6 → Faible
     *   - CrackLib rejette ET longueur < 6     → Très faible
     */
    private static String convertToScore(String password, String cracklibOutput) {

        // cracklibOutput est de la forme "leMotDePasse: OK" ou "leMotDePasse: it is too short"
        // On extrait la partie après ": " pour isoler le verdict
        String verdict = "";
        if (cracklibOutput != null && cracklibOutput.contains(": ")) {
            verdict = cracklibOutput.substring(cracklibOutput.indexOf(": ") + 2).trim();
        }

        boolean cracklibOk = verdict.equalsIgnoreCase("OK");
        int length = password.length();

        if (cracklibOk && length >= 16) {
            return "Très fort";
        } else if (cracklibOk && length >= 12) {
            return "Fort";
        } else if (cracklibOk && length >= 8) {
            return "Moyen";
        } else if (!cracklibOk && length >= 6) {
            return "Faible";
        } else {
            return "Très faible";
        }
    }
}