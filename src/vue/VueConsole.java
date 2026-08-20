package vue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/*
    * VueConsole rassemble les briques de saisie et d'affichage réutilisées par toutes les vues
    * de l'application : lire une ligne, un entier, un montant, une date, demander une
    * confirmation, afficher un message ou une erreur. Chaque vue d'écran (VuePrincipale,
    * VueTransaction...) en hérite pour composer son propre affichage à partir de ces briques,
    * sans dupliquer la logique de saisie.
    *
    * Une vue n'importe jamais modele.service : elle affiche et lit, elle ne déclenche aucun
    * traitement. C'est au contrôleur d'appeler les services et de transmettre le résultat à
    * la vue pour affichage.
*/
public class VueConsole {

    protected static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Statique et partagé par toutes les vues (VuePrincipale, VueTransaction...), pas un
    // attribut par instance : System.in est un flux unique pour tout le programme, et deux
    // Scanner ouverts dessus se volent des lignes l'un l'autre (chacun bufferise de son côté).
    // Un seul Scanner, commun à toutes les vues qui héritent de VueConsole, évite le problème.
    private static Scanner scanner = new Scanner(System.in);

    // Affiche un message d'information à l'utilisateur.
    public void afficherMessage(String message) {
        System.out.println(message);
    }

    // Affiche un message d'erreur. Toujours ce message-là, jamais la trace de l'exception
    // d'origine : c'est le contrôleur qui l'a attrapée et en a extrait le message lisible.
    public void afficherErreur(String message) {
        System.out.println("Erreur : " + message);
    }

    // Lit une ligne brute au clavier, débarrassée des espaces de bord.
    public String lireLigne(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    // Lit un entier au clavier, en boucle tant que la saisie n'en est pas un.
    public int lireEntier(String message) {
        while (true) {
            String saisie = lireLigne(message);
            try {
                return Integer.parseInt(saisie);
            } catch (NumberFormatException erreur) {
                afficherMessage("Veuillez saisir un nombre entier.");
            }
        }
    }

    // Lit un montant au clavier, en boucle tant que la saisie n'est pas un nombre strictement positif.
    public double lireMontant(String message) {
        while (true) {
            String saisie = lireLigne(message).replace(",", ".");
            try {
                double montant = Double.parseDouble(saisie);
                if (montant <= 0) {
                    afficherMessage("Le montant doit être strictement positif.");
                    continue;
                }
                return montant;
            } catch (NumberFormatException erreur) {
                afficherMessage("Montant invalide, veuillez saisir un nombre.");
            }
        }
    }

    // Lit une date au clavier (format JJ/MM/AAAA). Une saisie vide renvoie la date du jour.
    // En boucle tant que le format est incorrect ou que la date est dans le futur.
    public LocalDate lireDate(String message) {
        while (true) {
            String saisie = lireLigne(message);
            if (saisie.isEmpty()) {
                return LocalDate.now();
            }
            try {
                LocalDate date = LocalDate.parse(saisie, FORMAT_DATE);
                if (date.isAfter(LocalDate.now())) {
                    afficherMessage("La date ne peut pas être dans le futur.");
                    continue;
                }
                return date;
            } catch (DateTimeParseException erreur) {
                afficherMessage("Date invalide, format attendu JJ/MM/AAAA.");
            }
        }
    }

    // Message affiché quand l'utilisateur répond non à une confirmation, quel que soit l'écran :
    // générique, réutilisé par tous les écrans qui demandent confirmation avant d'enregistrer.
    public void afficherOperationAnnulee() {
        afficherMessage("Opération annulée.");
    }

    // Affiche un message et lit une réponse oui/non.
    public boolean confirmer(String message) {
        while (true) {
            String saisie = lireLigne(message + " (o/n) : ").toLowerCase();
            if (saisie.equals("o") || saisie.equals("oui")) {
                return true;
            }
            if (saisie.equals("n") || saisie.equals("non")) {
                return false;
            }
            afficherMessage("Réponse attendue : o ou n.");
        }
    }

    // Affiche l'échec de sauvegarde puis demande si l'utilisateur veut réessayer l'écriture sur
    // le disque. Générique et réutilisable par tout écran qui modifie réellement les données,
    // pas seulement les transactions : c'est pour ça qu'elle vit ici plutôt que dans une vue
    // d'écran particulière.
    public boolean demanderNouvelleTentativeSauvegarde(String messageErreur) {
        afficherMessage("Attention : " + messageErreur);
        return confirmer("Réessayer la sauvegarde ?");
    }

    public void afficherSauvegardeReussie() {
        afficherMessage("Sauvegarde réussie.");
    }

    // Affichée quand l'utilisateur renonce à réessayer : l'opération a bien eu lieu en mémoire,
    // seule l'écriture sur le disque a échoué (voir ErreurSauvegardeException).
    public void afficherSauvegardeAbandonnee() {
        afficherMessage("L'opération a bien été effectuée en mémoire, mais pas encore enregistrée sur le disque.");
    }
}
