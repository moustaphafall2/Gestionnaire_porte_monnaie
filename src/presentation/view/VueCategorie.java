package presentation.view;

import java.util.List;
import java.util.Set;

import domain.enumeration.Categorie;

/*
    * VueCategorie affiche l'écran "Gérer mes catégories" : consulter les catégories actives,
    * en activer une, en désactiver une.
*/
public class VueCategorie extends VueConsole {

    private void afficherMenuCategories() {
        afficherMessage("1. Activer une catégorie");
        afficherMessage("2. Désactiver une catégorie");
        afficherMessage("3. Retour");
    }

    public int demanderChoixMenu() {
        afficherMenuCategories();
        return lireEntier("Votre choix : ");
    }

    public void afficherCategoriesActives(Set<Categorie> actives) {
        if (actives.isEmpty()) {
            afficherMessage("Catégories actives : aucune.");
            return;
        }
        StringBuilder ligne = new StringBuilder("Catégories actives : ");
        boolean premiere = true;
        for (Categorie categorie : actives) {
            if (!premiere) {
                ligne.append(", ");
            }
            ligne.append(categorie.getLibelle());
            premiere = false;
        }
        afficherMessage(ligne.toString());
    }

    public void afficherToutesActives() {
        afficherMessage("Toutes les catégories sont déjà actives.");
    }

    public void afficherAucuneActive() {
        afficherMessage("Aucune catégorie active à désactiver.");
    }

    public Categorie demanderCategorie(List<Categorie> categories, String message) {
        afficherCategoriesNumerotees(categories);
        while (true) {
            int numero = lireEntier(message);
            if (numero >= 1 && numero <= categories.size()) {
                return categories.get(numero - 1);
            }
            afficherMessage("Numéro invalide.");
        }
    }

    private void afficherCategoriesNumerotees(List<Categorie> categories) {
        for (int i = 0; i < categories.size(); i++) {
            afficherMessage((i + 1) + ". " + categories.get(i).getLibelle());
        }
    }

    public void afficherCategorieActivee() {
        afficherMessage("Catégorie activée.");
    }

    public void afficherCategorieDesactivee() {
        afficherMessage("Catégorie désactivée.");
    }

    public Categorie demanderCategorieActivation(List<Categorie> disponibles) {
        if (disponibles.isEmpty()) {
            afficherToutesActives();
            return null;
        }
        return demanderCategorie(disponibles, "Numéro de la catégorie à activer : ");
    }

    public Categorie demanderCategorieDesactivation(Set<Categorie> actives) {
        if (actives.isEmpty()) {
            afficherAucuneActive();
            return null;
        }
        return demanderCategorie(List.copyOf(actives), "Numéro de la catégorie à désactiver : ");
    }
}
