package presentation.view;

import java.time.LocalDate;
import java.util.List;

import application.dto.TransactionDTO;
import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;

/*
    * VueTransaction affiche les écrans "Ajouter une dépense", "Ajouter un revenu" et "Voir
    * l'historique des transactions" (consultation, filtres, modification, suppression). Comme
    * VuePrincipale, elle hérite de VueConsole pour ses briques de saisie/affichage générales
    * (lireMontant, lireDate, confirmer...), gardées internes à cette classe : le contrôleur ne
    * connaît le texte d'aucune invite ni d'aucun message, il appelle des méthodes nommées pour
    * ce qu'elles demandent ou affichent, et ne reçoit que la valeur saisie ou rien du tout.
*/
public class VueTransaction extends VueConsole {

    public void afficherAucuneCategorieActive(TypeTransaction type) {
        String libelleType = (type == TypeTransaction.DEPENSE) ? "dépense" : "revenu";
        afficherMessage("Aucune catégorie de " + libelleType + " active. Activez-en une avant de continuer (menu Catégories).");
    }

    // Affiche les catégories numérotées et lit le choix de l'utilisateur, en boucle tant que le
    // numéro saisi ne correspond à aucune catégorie de la liste reçue.
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

    // Même texte pour l'ajout d'une dépense et d'un revenu : la date de la transaction se
    // demande de la même façon dans les deux cas.
    public LocalDate demanderDate() {
        return lireDate("Date (JJ/MM/AAAA, vide = aujourd'hui) : ");
    }

    // Une description vide n'est pas une erreur : la transaction n'a simplement pas de
    // description (champ facultatif). Même texte pour l'ajout d'une dépense et d'un revenu.
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

    // ----- 4. Historique -----

    public LocalDate demanderDateDebut() {
        return lireDate("Date de début (JJ/MM/AAAA) : ");
    }

    public LocalDate demanderDateFin() {
        return lireDate("Date de fin (JJ/MM/AAAA) : ");
    }

    // Un seul niveau : les six actions de l'écran historique, chacune avec sa propre méthode de
    // contrôleur (afficherHistorique, filtrerParDate, filtrerParCategorie, filtrerParType,
    // modifierTransaction, supprimerTransaction), donc chacune sa propre entrée ici.
    private void afficherMenuHistorique() {
        afficherMessage("1. Tout afficher");
        afficherMessage("2. Filtrer par date");
        afficherMessage("3. Filtrer par catégorie");
        afficherMessage("4. Filtrer par type");
        afficherMessage("5. Modifier une transaction");
        afficherMessage("6. Supprimer une transaction");
        afficherMessage("7. Retour");
    }

    // Affiche ce menu et lit le choix en un seul appel : Main fait directement son switch sur la
    // valeur renvoyée ici, sans méthode intermédiaire.
    public int demanderChoixMenuHistorique() {
        afficherMenuHistorique();
        return lireEntier("Votre choix : ");
    }

    // Affiche chaque transaction, une ligne par transaction, ou un message dédié si la liste
    // reçue est vide.
    public void afficherTransactions(List<TransactionDTO> transactions) {
        if (transactions.isEmpty()) {
            afficherMessage("Aucune transaction à afficher.");
            return;
        }
        for (TransactionDTO transaction : transactions) {
            afficherMessage(formaterLigne(transaction));
        }
    }

    // Reprend la mise en forme qui vivait auparavant dans Transaction.toString() : depuis
    // l'introduction des DTO, ni l'entité ni TransactionDTO ne portent de mise en forme,
    // c'est entièrement le rôle de la vue.
    private String formaterLigne(TransactionDTO transaction) {
        String descriptionAffichee = (transaction.getDescription() == null || transaction.getDescription().isBlank())
                ? "(sans description)" : transaction.getDescription();
        return transaction.getDate() + " - Transaction " + transaction.getId() + " " + transaction.getCategorie().getLibelle()
                + " - " + descriptionAffichee + ", montant = " + transaction.getMontant() + ", type = " + transaction.getType();
    }

    // Propose toutes les catégories de l'énumération (pas seulement les catégories actives) :
    // utile pour filtrer ou modifier une transaction enregistrée avec une catégorie
    // entretemps désactivée.
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
