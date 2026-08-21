package presentation.view;

import java.time.LocalDate;
import java.util.List;

import domain.entity.Transaction;
import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;

/*
    * VueTransaction affiche les écrans "Ajouter une dépense", "Ajouter un revenu" et "Voir
    * l'historique des transactions" (consultation, filtres, modification, suppression). Comme
    * VuePrincipale, elle hérite de VueConsole pour ses briques de saisie/affichage générales
    * (lireMontant, lireDate, confirmer...) et n'ajoute que ce qui est propre à ces écrans :
    * le contrôleur ne construit aucun texte à afficher, il ne fait que lui transmettre les
    * valeurs obtenues des services.
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

    // Une description vide n'est pas une erreur : la transaction n'a simplement pas de
    // description (champ facultatif).
    public String demanderDescription(String message) {
        String saisie = lireLigne(message);
        return saisie.isEmpty() ? null : saisie;
    }

    public void afficherRecapitulatif(double montant, Categorie categorie, LocalDate date) {
        afficherMessage(String.format("Récapitulatif : %.2f FCFA, %s, le %s", montant, categorie.getLibelle(), date.format(FORMAT_DATE)));
    }

    public void afficherAvertissementSoldeNegatif(double soldeApres) {
        afficherMessage(String.format("Attention : cette dépense rendra votre solde négatif (nouveau solde : %.2f FCFA).", soldeApres));
    }

    public void afficherDepenseEnregistree() {
        afficherMessage("Dépense enregistrée.");
    }

    public void afficherRevenuEnregistre() {
        afficherMessage("Revenu enregistré.");
    }

    // ----- 4. Historique -----

    public void afficherMenuHistorique() {
        afficherMessage("1. Tout afficher");
        afficherMessage("2. Filtrer par date");
        afficherMessage("3. Filtrer par catégorie");
        afficherMessage("4. Filtrer par type");
        afficherMessage("5. Modifier ou supprimer une transaction");
        afficherMessage("6. Retour");
    }

    public void afficherMenuModifierSupprimer() {
        afficherMessage("1. Modifier");
        afficherMessage("2. Supprimer");
    }

    // Affiche chaque transaction (via son toString(), déjà écrit dans l'entité), ou un message
    // dédié si la liste reçue est vide.
    public void afficherTransactions(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            afficherMessage("Aucune transaction à afficher.");
            return;
        }
        for (Transaction transaction : transactions) {
            afficherMessage(transaction.toString());
        }
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

    public void afficherTransactionModifiee() {
        afficherMessage("Transaction modifiée.");
    }

    public void afficherTransactionSupprimee() {
        afficherMessage("Transaction supprimée.");
    }
}
