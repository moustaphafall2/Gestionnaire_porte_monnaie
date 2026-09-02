package presentation.controller;

import domain.enumeration.Categorie;
import application.service.interfaces.IServiceCategorie;
import presentation.view.VueCategorie;

/*
    * ControleurCategorie enchaîne l'écran "Gérer mes catégories" : activer ou désactiver une
    * catégorie.
*/
public class ControleurCategorie {
    private final VueCategorie vueCategorie;
    private final IServiceCategorie serviceCategorie;

    public ControleurCategorie(VueCategorie vueCategorie, IServiceCategorie serviceCategorie) {
        this.vueCategorie = vueCategorie;
        this.serviceCategorie = serviceCategorie;
    }

    public void activerCategorie() {
        Categorie categorie = vueCategorie.demanderCategorieActivation(serviceCategorie.getCategoriesDisponibles());
        if (categorie == null) {
            return;
        }
        serviceCategorie.activerCategorie(categorie);
        vueCategorie.afficherCategorieActivee();
    }

    public void desactiverCategorie() {
        Categorie categorie = vueCategorie.demanderCategorieDesactivation(serviceCategorie.getCategoriesActives());
        if (categorie == null) {
            return;
        }
        serviceCategorie.desactiverCategorie(categorie);
        vueCategorie.afficherCategorieDesactivee();
    }
}
