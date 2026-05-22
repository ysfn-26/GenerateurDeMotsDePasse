import java.io.*;
import java.net.*;
import java.util.*;

/**
 * PasswordApp.java — Application CLI de génération de mots de passe.
 *
 * Fonctionnement :
 *   1. L'utilisateur choisit ses options (longueur, types de caractères, quantité).
 *   2. Le programme génère les mots de passe demandés.
 *   3. Pour chaque mot de passe, une connexion TCP est ouverte vers le conteneur
 *      Docker (AuditServer) sur le port 12345 pour obtenir le score de robustesse.
 *   4. Les résultats sont affichés dans un tableau formaté.
 */
public class PasswordApp {

    // Adresse et port du serveur d'audit Docker
    private static final String AUDIT_HOST = "localhost";
    private static final int    AUDIT_PORT = 12345;

    // Jeux de caractères disponibles pour la génération
    private static final String MAJUSCULES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String MINUSCULES = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHIFFRES   = "0123456789";
    private static final String SYMBOLES   = "!@#$%^&*()-_=+[]{}|;:',.<>?/`~";

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     Générateur de Mots de Passe      ║");
        System.out.println("║         Audit via Docker             ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();

        // ── Étape 1 : longueur du mot de passe ──────────────────────────────
        int longueur = 0;
        while (longueur < 4 || longueur > 64) {
            System.out.print("Longueur du mot de passe (4-64) : ");
            try {
                longueur = Integer.parseInt(scanner.nextLine().trim());
                if (longueur < 4 || longueur > 64) {
                    System.out.println("  ⚠ Valeur hors limites, réessaie.");
                }
            } catch (NumberFormatException e) {
                System.out.println("  ⚠ Entre un nombre entier.");
            }
        }

        // ── Étape 2 : types de caractères ───────────────────────────────────
        boolean useMaj = demanderOuiNon(scanner, "Inclure des majuscules ? (o/n) : ");
        boolean useMin = demanderOuiNon(scanner, "Inclure des minuscules ? (o/n) : ");
        boolean useChif = demanderOuiNon(scanner, "Inclure des chiffres ?    (o/n) : ");
        boolean useSym  = demanderOuiNon(scanner, "Inclure des symboles ?    (o/n) : ");

        // Au moins un type de caractère doit être sélectionné
        if (!useMaj && !useMin && !useChif && !useSym) {
            System.out.println("\n⚠ Aucun type sélectionné. Minuscules activées par défaut.");
            useMin = true;
        }

        // ── Étape 3 : nombre de mots de passe (mode rafale) ─────────────────
        int quantite = 0;
        while (quantite < 1 || quantite > 50) {
            System.out.print("Combien de mots de passe ? (1-50) : ");
            try {
                quantite = Integer.parseInt(scanner.nextLine().trim());
                if (quantite < 1 || quantite > 50) {
                    System.out.println("  ⚠ Valeur hors limites, réessaie.");
                }
            } catch (NumberFormatException e) {
                System.out.println("  ⚠ Entre un nombre entier.");
            }
        }

        // ── Étape 4 : construction du pool de caractères ─────────────────────
        StringBuilder pool = new StringBuilder();
        if (useMaj)  pool.append(MAJUSCULES);
        if (useMin)  pool.append(MINUSCULES);
        if (useChif) pool.append(CHIFFRES);
        if (useSym)  pool.append(SYMBOLES);

        String poolStr = pool.toString();

        // ── Étape 5 : génération et audit ────────────────────────────────────
        System.out.println();
        System.out.println("┌─────┬──────────────────────────────────┬─────────────┐");
        System.out.println("│  N° │ Mot de passe                     │ Score       │");
        System.out.println("├─────┼──────────────────────────────────┼─────────────┤");

        Random random = new Random();

        for (int i = 1; i <= quantite; i++) {

            // Génération du mot de passe aléatoire depuis le pool
            String motDePasse = genererMotDePasse(poolStr, longueur, random);

            // Envoi au conteneur Docker pour audit et récupération du score
            String score = auditer(motDePasse);

            // Affichage d'une ligne du tableau (colonnes alignées)
            System.out.printf("│ %3d │ %-32s │ %-11s │%n", i, motDePasse, score);
        }

        System.out.println("└─────┴──────────────────────────────────┴─────────────┘");
        System.out.println();
        scanner.close();
    }

    /**
     * Génère un mot de passe aléatoire de la longueur demandée
     * en piochant dans le pool de caractères fourni.
     */
    private static String genererMotDePasse(String pool, int longueur, Random random) {
        StringBuilder sb = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            // Choisit un index aléatoire dans le pool et ajoute le caractère
            sb.append(pool.charAt(random.nextInt(pool.length())));
        }
        return sb.toString();
    }

    /**
     * Ouvre une connexion TCP vers le serveur AuditServer dans le conteneur Docker,
     * envoie le mot de passe, et retourne le score reçu en réponse.
     * En cas d'échec de connexion, retourne "Erreur audit" pour ne pas
     * bloquer l'affichage des autres mots de passe.
     */
    private static String auditer(String motDePasse) {
        try (
                Socket socket = new Socket(AUDIT_HOST, AUDIT_PORT);
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), "UTF-8"))
        ) {
            // Envoi du mot de passe au serveur Docker (suivi d'un retour à la ligne)
            out.println(motDePasse);

            // Lecture et retour du score renvoyé par AuditServer
            return in.readLine();

        } catch (ConnectException e) {
            // Le conteneur Docker n'est pas démarré ou le port est inaccessible
            return "Docker KO";
        } catch (IOException e) {
            // Toute autre erreur réseau
            return "Erreur audit";
        }
    }

    /**
     * Pose une question oui/non à l'utilisateur et retourne true pour "o", false pour "n".
     * Répète la question tant que la saisie n'est pas valide.
     */
    private static boolean demanderOuiNon(Scanner scanner, String question) {
        while (true) {
            System.out.print(question);
            String reponse = scanner.nextLine().trim().toLowerCase();
            if (reponse.equals("o")) return true;
            if (reponse.equals("n")) return false;
            System.out.println("  ⚠ Tape 'o' pour oui ou 'n' pour non.");
        }
    }
}