package vue;

import java.time.LocalDate;
import java.util.List;

import modele.enumeration.Categorie;
import modele.enumeration.TypeTransaction;

/*
    * VueTransaction affiche les écrans "Ajouter une dépense" et "Ajouter un revenu". Comme
    * VuePrincipale, elle hérite de VueConsole pour ses briques de saisie/affichage générales
    * (lireMontant, lireDate, confirmer...) et n'ajoute que ce qui est propre à ces deux écrans :
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

    public void afficherOperationAnnulee() {
        afficherMessage("Opération annulée.");
    }

    public void afficherDepenseEnregistree() {
        afficherMessage("Dépense enregistrée.");
    }

    public void afficherRevenuEnregistre() {
        afficherMessage("Revenu enregistré.");
    }
}
