package presentation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import metier.Categorie;
import metier.Epargne;
import metier.ErreurSauvegardeException;
import metier.MouvementEpargne;
import metier.Portefeuille;
import metier.Transaction;
import metier.TypeTransaction;

/*
    * La classe Menu gère uniquement l'interaction avec l'utilisateur dans la console :
    * afficher les options, lire les saisies, afficher les résultats et les messages
    * d'erreur ou de confirmation. Elle ne contient aucune logique métier : chaque action
    * se traduit par un appel à une méthode de Portefeuille, qui seule décide si
    * l'opération est valide (les exceptions qu'elle lève sont attrapées ici pour être
    * transformées en messages lisibles).
*/
public class Menu {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Portefeuille portefeuille;
    private Scanner scanner;

    public Menu(Portefeuille portefeuille) {
        this.portefeuille = portefeuille;
        this.scanner = new Scanner(System.in);
    }

    // Boucle principale : affiche le menu, lit le choix, exécute l'action correspondante,
    // jusqu'à ce que l'utilisateur choisisse "Quitter".
    public void lancer() {
        boolean continuer = true;

        while (continuer) {
            afficherMenuPrincipal();
            int choix = lireEntier("Votre choix : ");

            // Attrapé ici, au niveau le plus haut, pour couvrir toutes les opérations d'un
            // coup : quelle que soit l'action en cours, un échec d'écriture disque ne doit
            // jamais faire planter l'application. Les données restent valides en mémoire
            // pour la suite de la session (l'opération elle-même a déjà eu lieu avant l'échec
            // de la sauvegarde), seule l'écriture sur le disque a échoué.
            try {
                switch (choix) {
                    case 1 -> gererVoirSolde();
                    case 2 -> gererAjouterDepense();
                    case 3 -> gererAjouterRevenu();
                    case 4 -> gererHistorique();
                    case 5 -> gererObjectifsEpargne();
                    case 6 -> gererCategories();
                    case 7 -> gererStatistiques();
                    case 8 -> continuer = false;
                    default -> System.out.println("Choix invalide, veuillez recommencer.");
                }
            } catch (ErreurSauvegardeException erreur) {
                System.out.println("Attention : " + erreur.getMessage());
                System.out.println("L'opération a bien été effectuée en mémoire, mais pas encore enregistrée sur le disque.");
            }
        }

        System.out.println("Au revoir !");
    }

    public void afficherMenuPrincipal() {
        System.out.println();
        System.out.println("=== GESTION DE PORTE-MONNAIE ===");
        System.out.println("1. Voir le solde");
        System.out.println("2. Ajouter une dépense");
        System.out.println("3. Ajouter un revenu");
        System.out.println("4. Voir l'historique des transactions");
        System.out.println("5. Gérer mes objectifs d'épargne");
        System.out.println("6. Gérer mes catégories");
        System.out.println("7. Voir les statistiques");
        System.out.println("8. Quitter");
    }

    // ----- 1. Solde -----

    public void gererVoirSolde() {
        System.out.printf("Solde disponible : %.2f FCFA%n", portefeuille.getSoldeDisponible());
        System.out.printf("Total épargné : %.2f FCFA%n", portefeuille.getTotalEpargne());
    }

    // ----- 2 et 3. Ajouter une dépense / un revenu -----

    public void gererAjouterDepense() {
        if (!portefeuille.aCategorieActiveDeType(TypeTransaction.DEPENSE)) {
            System.out.println("Aucune catégorie de dépense active. Activez-en une avant de continuer (menu Catégories).");
            return;
        }

        Categorie categorie = demanderCategorie(TypeTransaction.DEPENSE);
        double montant = demanderMontant("Montant de la dépense : ");
        LocalDate date = demanderDate("Date (JJ/MM/AAAA, vide = aujourd'hui) : ");
        String description = demanderTexte("Description (facultative) : ");

        double soldeApres = portefeuille.soldeApresDepense(montant);
        System.out.printf("Récapitulatif : %.2f FCFA, %s, le %s%n", montant, categorie.getLibelle(), date.format(FORMAT_DATE));
        if (soldeApres < 0) {
            System.out.printf("Attention : cette dépense rendra votre solde négatif (nouveau solde : %.2f FCFA).%n", soldeApres);
        }

        if (!demanderConfirmation("Confirmer l'enregistrement de cette dépense ?")) {
            System.out.println("Opération annulée.");
            return;
        }

        portefeuille.ajouterDepense(montant, categorie, date, description);
        System.out.println("Dépense enregistrée.");
    }

    public void gererAjouterRevenu() {
        if (!portefeuille.aCategorieActiveDeType(TypeTransaction.REVENU)) {
            System.out.println("Aucune catégorie de revenu active. Activez-en une avant de continuer (menu Catégories).");
            return;
        }

        Categorie categorie = demanderCategorie(TypeTransaction.REVENU);
        double montant = demanderMontant("Montant du revenu : ");
        LocalDate date = demanderDate("Date (JJ/MM/AAAA, vide = aujourd'hui) : ");
        String description = demanderTexte("Description (facultative) : ");

        System.out.printf("Récapitulatif : %.2f FCFA, %s, le %s%n", montant, categorie.getLibelle(), date.format(FORMAT_DATE));
        if (!demanderConfirmation("Confirmer l'enregistrement de ce revenu ?")) {
            System.out.println("Opération annulée.");
            return;
        }

        portefeuille.ajouterRevenu(montant, categorie, date, description);
        System.out.println("Revenu enregistré.");
    }

    // ----- 4. Historique -----

    public void gererHistorique() {
        System.out.println("1. Tout afficher");
        System.out.println("2. Filtrer par date");
        System.out.println("3. Filtrer par catégorie");
        System.out.println("4. Filtrer par type");
        System.out.println("5. Modifier ou supprimer une transaction");
        System.out.println("6. Retour");
        int choix = lireEntier("Votre choix : ");

        List<Transaction> resultat;
        switch (choix) {
            case 1 -> resultat = portefeuille.getHistorique();
            case 2 -> {
                LocalDate debut = demanderDate("Date de début (JJ/MM/AAAA) : ");
                LocalDate fin = demanderDate("Date de fin (JJ/MM/AAAA) : ");
                resultat = portefeuille.filtrerParDate(debut, fin);
            }
            case 3 -> resultat = portefeuille.filtrerParCategorie(demanderCategorieParmiToutes());
            case 4 -> resultat = portefeuille.filtrerParType(demanderType());
            case 5 -> {
                gererModificationSuppressionTransaction();
                return;
            }
            default -> {
                return;
            }
        }

        afficherTransactions(resultat);
    }

    private void afficherTransactions(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            System.out.println("Aucune transaction à afficher.");
            return;
        }
        for (Transaction transaction : transactions) {
            System.out.println(transaction);
        }
    }

    public void gererModificationSuppressionTransaction() {
        afficherTransactions(portefeuille.getHistorique());
        int id = lireEntier("Identifiant de la transaction à modifier/supprimer (0 pour annuler) : ");
        if (id == 0) {
            return;
        }

        System.out.println("1. Modifier");
        System.out.println("2. Supprimer");
        int choix = lireEntier("Votre choix : ");

        try {
            if (choix == 1) {
                double montant = demanderMontant("Nouveau montant : ");
                LocalDate date = demanderDate("Nouvelle date (JJ/MM/AAAA, vide = aujourd'hui) : ");
                Categorie categorie = demanderCategorieParmiToutes();
                String description = demanderTexte("Nouvelle description (facultative) : ");
                portefeuille.modifierTransaction(id, montant, categorie, date, description);
                System.out.println("Transaction modifiée.");
            } else if (choix == 2) {
                if (demanderConfirmation("Confirmer la suppression de cette transaction ?")) {
                    portefeuille.supprimerTransaction(id);
                    System.out.println("Transaction supprimée.");
                } else {
                    System.out.println("Opération annulée.");
                }
            }
        } catch (IllegalArgumentException | IllegalStateException erreur) {
            System.out.println("Erreur : " + erreur.getMessage());
        }
    }

    // ----- 5. Objectifs d'épargne -----

    public void gererObjectifsEpargne() {
        System.out.println("1. Créer un objectif");
        System.out.println("2. Contribuer à un objectif");
        System.out.println("3. Retirer d'un objectif");
        System.out.println("4. Voir mes objectifs");
        System.out.println("5. Supprimer un objectif");
        System.out.println("6. Retour");
        int choix = lireEntier("Votre choix : ");

        try {
            switch (choix) {
                case 1 -> {
                    String nom = demanderTexte("Nom de l'objectif : ");
                    double montantCible = demanderMontant("Montant cible : ");
                    LocalDate dateLimite = demanderDateFacultative("Date limite (JJ/MM/AAAA, facultative, vide = aucune) : ");
                    System.out.printf("Récapitulatif : \"%s\", cible %.2f FCFA%n", nom, montantCible);
                    if (!demanderConfirmation("Confirmer la création de cet objectif ?")) {
                        System.out.println("Opération annulée.");
                        return;
                    }
                    portefeuille.creerObjectif(nom, montantCible, dateLimite);
                    System.out.println("Objectif créé.");
                }
                case 2 -> {
                    afficherObjectifs();
                    int id = lireEntier("Identifiant de l'objectif : ");
                    Epargne objectif = portefeuille.getObjectif(id);
                    double montant = demanderMontant("Montant à ajouter : ");
                    System.out.printf("Solde disponible actuel : %.2f FCFA%n", portefeuille.getSoldeDisponible());
                    if (objectif.depasseraCible(montant)) {
                        System.out.println("Attention : cette contribution dépassera le montant cible de l'objectif.");
                    }
                    LocalDate date = demanderDate("Date (JJ/MM/AAAA, vide = aujourd'hui) : ");
                    System.out.printf("Récapitulatif : %.2f FCFA vers \"%s\" le %s%n", montant, objectif.getNom(), date.format(FORMAT_DATE));
                    if (!demanderConfirmation("Confirmer cette contribution ?")) {
                        System.out.println("Opération annulée.");
                        return;
                    }
                    portefeuille.contribuerObjectif(id, montant, date);
                    System.out.println("Contribution enregistrée.");
                }
                case 3 -> {
                    afficherObjectifs();
                    int id = lireEntier("Identifiant de l'objectif : ");
                    Epargne objectif = portefeuille.getObjectif(id);
                    double montant = demanderMontant("Montant à retirer : ");
                    LocalDate date = demanderDate("Date (JJ/MM/AAAA, vide = aujourd'hui) : ");
                    System.out.printf("Récapitulatif : %.2f FCFA retirés de \"%s\" le %s%n", montant, objectif.getNom(), date.format(FORMAT_DATE));
                    if (!demanderConfirmation("Confirmer ce retrait ?")) {
                        System.out.println("Opération annulée.");
                        return;
                    }
                    portefeuille.retirerObjectif(id, montant, date);
                    System.out.println("Retrait enregistré.");
                }
                case 4 -> {
                    afficherObjectifs();
                    if (!portefeuille.getObjectifs().isEmpty()) {
                        int id = lireEntier("Identifiant de l'objectif à détailler (0 pour revenir) : ");
                        if (id != 0) {
                            afficherMouvements(portefeuille.getObjectif(id));
                        }
                    }
                }
                case 5 -> {
                    afficherObjectifs();
                    int id = lireEntier("Identifiant de l'objectif à supprimer : ");
                    if (demanderConfirmation("Confirmer la suppression de cet objectif ?")) {
                        portefeuille.supprimerObjectif(id);
                        System.out.println("Objectif supprimé.");
                    } else {
                        System.out.println("Opération annulée.");
                    }
                }
                default -> { }
            }
        } catch (IllegalArgumentException | IllegalStateException erreur) {
            System.out.println("Erreur : " + erreur.getMessage());
        }
    }

    private void afficherObjectifs() {
        List<Epargne> objectifs = portefeuille.getObjectifs();
        if (objectifs.isEmpty()) {
            System.out.println("Aucun objectif d'épargne pour le moment.");
            return;
        }
        for (Epargne objectif : objectifs) {
            System.out.println(objectif.getId() + " - " + objectif);
        }
    }

    // Détail des contributions/retraits d'un objectif. Ces mouvements ne sont jamais mélangés
    // à l'historique des transactions (menu 4) : ce n'est ni une dépense ni un revenu.
    private void afficherMouvements(Epargne objectif) {
        List<MouvementEpargne> mouvements = objectif.getMouvements();
        if (mouvements.isEmpty()) {
            System.out.println("Aucun mouvement pour le moment sur \"" + objectif.getNom() + "\".");
            return;
        }
        System.out.println("Mouvements de \"" + objectif.getNom() + "\" :");
        for (MouvementEpargne mouvement : mouvements) {
            System.out.println("  " + mouvement);
        }
    }

    // ----- 6. Catégories -----

    public void gererCategories() {
        System.out.println("Catégories actives : " + portefeuille.getCategoriesActives());
        System.out.println("1. Activer une catégorie");
        System.out.println("2. Désactiver une catégorie");
        System.out.println("3. Retour");
        int choix = lireEntier("Votre choix : ");

        if (choix == 1) {
            List<Categorie> disponibles = portefeuille.getCategoriesDisponibles();
            if (disponibles.isEmpty()) {
                System.out.println("Toutes les catégories sont déjà actives.");
                return;
            }
            afficherCategoriesNumerotees(disponibles);
            int numero = lireEntier("Numéro de la catégorie à activer : ");
            if (numero < 1 || numero > disponibles.size()) {
                System.out.println("Numéro invalide.");
                return;
            }
            portefeuille.activerCategorie(disponibles.get(numero - 1));
            System.out.println("Catégorie activée.");
        } else if (choix == 2) {
            List<Categorie> actives = List.copyOf(portefeuille.getCategoriesActives());
            if (actives.isEmpty()) {
                System.out.println("Aucune catégorie active à désactiver.");
                return;
            }
            afficherCategoriesNumerotees(actives);
            int numero = lireEntier("Numéro de la catégorie à désactiver : ");
            if (numero < 1 || numero > actives.size()) {
                System.out.println("Numéro invalide.");
                return;
            }
            portefeuille.desactiverCategorie(actives.get(numero - 1));
            System.out.println("Catégorie désactivée.");
        }
    }

    private void afficherCategoriesNumerotees(List<Categorie> categories) {
        for (int i = 0; i < categories.size(); i++) {
            System.out.println((i + 1) + ". " + categories.get(i).getLibelle());
        }
    }

    // ----- 7. Statistiques -----

    public void gererStatistiques() {
        LocalDate debut = demanderDate("Date de début (JJ/MM/AAAA) : ");
        LocalDate fin = demanderDate("Date de fin (JJ/MM/AAAA) : ");

        Map<Categorie, Double> totauxParCategorie = portefeuille.getTotalParCategorie(debut, fin);
        System.out.println("Total dépensé par catégorie :");
        if (totauxParCategorie.isEmpty()) {
            System.out.println("  Aucune dépense sur cette période.");
        } else {
            for (Map.Entry<Categorie, Double> entree : totauxParCategorie.entrySet()) {
                System.out.printf("  %s : %.2f FCFA%n", entree.getKey().getLibelle(), entree.getValue());
            }
        }

        double[] totaux = portefeuille.getTotalRevenusEtDepenses(debut, fin);
        System.out.printf("Total des revenus : %.2f FCFA%n", totaux[0]);
        System.out.printf("Total des dépenses : %.2f FCFA%n", totaux[1]);
    }

    // ----- Saisies utilisateur -----

    // Lit un montant au clavier, en boucle tant que la saisie n'est pas un nombre strictement positif.
    public double demanderMontant(String message) {
        while (true) {
            System.out.print(message);
            String saisie = scanner.nextLine().trim().replace(",", ".");
            try {
                double montant = Double.parseDouble(saisie);
                if (montant <= 0) {
                    System.out.println("Le montant doit être strictement positif.");
                    continue;
                }
                return montant;
            } catch (NumberFormatException erreur) {
                System.out.println("Montant invalide, veuillez saisir un nombre.");
            }
        }
    }

    // Lit une date au clavier (format JJ/MM/AAAA). Une saisie vide renvoie la date du jour.
    // En boucle tant que le format est incorrect ou que la date est dans le futur.
    public LocalDate demanderDate(String message) {
        while (true) {
            System.out.print(message);
            String saisie = scanner.nextLine().trim();
            if (saisie.isEmpty()) {
                return LocalDate.now();
            }
            try {
                LocalDate date = LocalDate.parse(saisie, FORMAT_DATE);
                if (date.isAfter(LocalDate.now())) {
                    System.out.println("La date ne peut pas être dans le futur.");
                    continue;
                }
                return date;
            } catch (DateTimeParseException erreur) {
                System.out.println("Date invalide, format attendu JJ/MM/AAAA.");
            }
        }
    }

    // Variante de demanderDate() pour les champs facultatifs (ex. date limite d'un objectif) :
    // une saisie vide renvoie null au lieu de la date du jour, et le futur est autorisé.
    private LocalDate demanderDateFacultative(String message) {
        System.out.print(message);
        String saisie = scanner.nextLine().trim();
        if (saisie.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(saisie, FORMAT_DATE);
        } catch (DateTimeParseException erreur) {
            System.out.println("Date invalide, ignorée.");
            return null;
        }
    }

    // Affiche les catégories actives correspondant au type et lit le choix de l'utilisateur.
    public Categorie demanderCategorie(TypeTransaction type) {
        List<Categorie> disponibles = portefeuille.getCategoriesActives().stream()
                .filter(categorie -> categorie.getType() == type)
                .toList();

        while (true) {
            afficherCategoriesNumerotees(disponibles);
            int numero = lireEntier("Numéro de la catégorie : ");
            if (numero >= 1 && numero <= disponibles.size()) {
                return disponibles.get(numero - 1);
            }
            System.out.println("Numéro invalide.");
        }
    }

    // Variante utilisée pour filtrer/modifier : propose toutes les catégories de la liste
    // complète (pas seulement les catégories actives), utile pour retrouver une transaction
    // enregistrée avec une catégorie entretemps désactivée.
    private Categorie demanderCategorieParmiToutes() {
        List<Categorie> toutes = List.of(Categorie.values());
        while (true) {
            afficherCategoriesNumerotees(toutes);
            int numero = lireEntier("Numéro de la catégorie : ");
            if (numero >= 1 && numero <= toutes.size()) {
                return toutes.get(numero - 1);
            }
            System.out.println("Numéro invalide.");
        }
    }

    private TypeTransaction demanderType() {
        while (true) {
            System.out.println("1. Dépense");
            System.out.println("2. Revenu");
            int choix = lireEntier("Votre choix : ");
            if (choix == 1) {
                return TypeTransaction.DEPENSE;
            }
            if (choix == 2) {
                return TypeTransaction.REVENU;
            }
            System.out.println("Choix invalide.");
        }
    }

    // Affiche un message et lit une réponse oui/non.
    public boolean demanderConfirmation(String message) {
        while (true) {
            System.out.print(message + " (o/n) : ");
            String saisie = scanner.nextLine().trim().toLowerCase();
            if (saisie.equals("o") || saisie.equals("oui")) {
                return true;
            }
            if (saisie.equals("n") || saisie.equals("non")) {
                return false;
            }
            System.out.println("Réponse attendue : o ou n.");
        }
    }

    private String demanderTexte(String message) {
        System.out.print(message);
        String saisie = scanner.nextLine().trim();
        return saisie.isEmpty() ? null : saisie;
    }

    // Lit un entier au clavier, en boucle tant que la saisie n'en est pas un.
    private int lireEntier(String message) {
        while (true) {
            System.out.print(message);
            String saisie = scanner.nextLine().trim();
            try {
                return Integer.parseInt(saisie);
            } catch (NumberFormatException erreur) {
                System.out.println("Veuillez saisir un nombre entier.");
            }
        }
    }
}
