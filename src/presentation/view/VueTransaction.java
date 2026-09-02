package presentation.view;

import java.time.LocalDate;
import java.util.List;

import application.dto.TransactionDTO;
import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;

/*
    * VueTransaction affiche les écrans "Ajouter une dépense", "Ajouter un revenu" et "Voir
    * l'historique des transactions" (consultation, filtres, modification, suppression). Hérite
    * de VueConsole pour ses briques de saisie/affichage générales.
*/
public class VueTransaction extends VueConsole {

    public void afficherAucuneCategorieActive(TypeTransaction type) {
        String libelleType = (type == TypeTransaction.DEPENSE) ? "dépense" : "revenu";
        afficherMessage("Aucune catégorie de " + libelleType + " active. Activez-en une avant de continuer (menu Catégories).");
    }

    public Categorie demanderCategorie(List<Categorie> disponibles) {
        afficherCategoriesNumerotees(disponibles);
        while (true) {
            int numero = lireEntier("Numéro de la catégorie : ");
            if (numero >= 1 && numero <= disponibles.size()) {
                return disponibles.get(numero - 1);
            }
            afficherMessage("Numéro invalide.");
        }
    }

    private void afficherCategoriesNumerotees(List<Categorie> categories) {
        for (int i = 0; i < categories.size(); i++) {
            afficherMessage((i + 1) + ". " + categories.get(i).getLibelle());
        }
    }

    public double demanderMontantDepense() {
        return lireMontant("Montant de la dépense : ");
    }

    public double demanderMontantRevenu() {
        return lireMontant("Montant du revenu : ");
    }

    public LocalDate demanderDate() {
        return lireDate("Date (JJ/MM/AAAA, vide = aujourd'hui) : ");
    }

    public String demanderDescription() {
        String saisie = lireLigne("Description (facultative) : ");
        return saisie.isEmpty() ? null : saisie;
    }

    public void afficherRecapitulatif(double montant, Categorie categorie, LocalDate date) {
        afficherMessage(String.format("Récapitulatif : %.2f FCFA, %s, le %s", montant, categorie.getLibelle(), date.format(FORMAT_DATE)));
    }

    public void afficherAvertissementSoldeNegatif(double soldeApres) {
        afficherMessage(String.format("Attention : cette dépense rendra votre solde négatif (nouveau solde : %.2f FCFA).", soldeApres));
    }

    public boolean demanderConfirmationDepense() {
        return confirmer("Confirmer l'enregistrement de cette dépense ?");
    }

    public boolean demanderConfirmationRevenu() {
        return confirmer("Confirmer l'enregistrement de ce revenu ?");
    }

    public void afficherDepenseEnregistree() {
        afficherMessage("Dépense enregistrée.");
    }

    public void afficherRevenuEnregistre() {
        afficherMessage("Revenu enregistré.");
    }

    public LocalDate demanderDateDebut() {
        return lireDate("Date de début (JJ/MM/AAAA) : ");
    }

    public LocalDate demanderDateFin() {
        return lireDate("Date de fin (JJ/MM/AAAA) : ");
    }

    private void afficherMenuHistorique() {
        afficherMessage("1. Tout afficher");
        afficherMessage("2. Filtrer par date");
        afficherMessage("3. Filtrer par catégorie");
        afficherMessage("4. Filtrer par type");
        afficherMessage("5. Modifier une transaction");
        afficherMessage("6. Supprimer une transaction");
        afficherMessage("7. Retour");
    }

    public int demanderChoixMenuHistorique() {
        afficherMenuHistorique();
        return lireEntier("Votre choix : ");
    }

    public void afficherTransactions(List<TransactionDTO> transactions) {
        if (transactions.isEmpty()) {
            afficherMessage("Aucune transaction à afficher.");
            return;
        }
        for (TransactionDTO transaction : transactions) {
            afficherMessage(formaterLigne(transaction));
        }
    }

    private String formaterLigne(TransactionDTO transaction) {
        String descriptionAffichee = (transaction.getDescription() == null || transaction.getDescription().isBlank())
                ? "(sans description)" : transaction.getDescription();
        return transaction.getDate() + " - Transaction " + transaction.getId() + " " + transaction.getCategorie().getLibelle()
                + " - " + descriptionAffichee + ", montant = " + transaction.getMontant() + ", type = " + transaction.getType();
    }

    // Propose toutes les catégories, pas seulement les actives : utile pour une transaction dont
    // la catégorie a été désactivée depuis.
    public Categorie demanderCategorieParmiToutes() {
        return demanderCategorie(List.of(Categorie.values()));
    }

    public TypeTransaction demanderType() {
        while (true) {
            afficherMessage("1. Dépense");
            afficherMessage("2. Revenu");
            int choix = lireEntier("Votre choix : ");
            if (choix == 1) {
                return TypeTransaction.DEPENSE;
            }
            if (choix == 2) {
                return TypeTransaction.REVENU;
            }
            afficherMessage("Choix invalide.");
        }
    }

    public int demanderIdentifiantAModifier() {
        return lireEntier("Identifiant de la transaction à modifier (0 pour annuler) : ");
    }

    public double demanderNouveauMontant() {
        return lireMontant("Nouveau montant : ");
    }

    public LocalDate demanderNouvelleDate() {
        return lireDate("Nouvelle date (JJ/MM/AAAA, vide = aujourd'hui) : ");
    }

    public String demanderNouvelleDescription() {
        String saisie = lireLigne("Nouvelle description (facultative) : ");
        return saisie.isEmpty() ? null : saisie;
    }

    public void afficherTransactionModifiee() {
        afficherMessage("Transaction modifiée.");
    }

    public int demanderIdentifiantASupprimer() {
        return lireEntier("Identifiant de la transaction à supprimer (0 pour annuler) : ");
    }

    public boolean demanderConfirmationSuppression() {
        return confirmer("Confirmer la suppression de cette transaction ?");
    }

    public void afficherTransactionSupprimee() {
        afficherMessage("Transaction supprimée.");
    }
}
