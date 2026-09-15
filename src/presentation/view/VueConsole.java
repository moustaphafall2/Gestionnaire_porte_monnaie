package presentation.view;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/*
    * VueConsole rassemble les briques de saisie et d'affichage réutilisées par toutes les vues :
    * lire une ligne, un entier, un montant, une date, demander une confirmation, afficher un
    * message ou une erreur.
*/
public class VueConsole {

    protected static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Scanner unique et statique, partagé par toutes les vues : deux Scanner ouverts sur
    // System.in se voleraient des lignes l'un l'autre.
    private static Scanner scanner = new Scanner(System.in);

    public void afficherMessage(String message) {
        System.out.println(message);
    }

    public void afficherErreur(String message) {
        System.out.println("Erreur : " + message);
    }

    public String lireLigne(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

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

    public BigDecimal lireMontant(String message) {
        while (true) {
            String saisie = lireLigne(message).replace(",", ".");
            try {
                return new BigDecimal(saisie).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException erreur) {
                afficherMessage("Montant invalide, veuillez saisir un nombre.");
            }
        }
    }

    public LocalDate lireDate(String message) {
        while (true) {
            String saisie = lireLigne(message);
            if (saisie.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(saisie, FORMAT_DATE);
            } catch (DateTimeParseException erreur) {
                afficherMessage("Date invalide, format attendu JJ/MM/AAAA.");
            }
        }
    }

    public void afficherOperationAnnulee() {
        afficherMessage("Opération annulée.");
    }

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
}
