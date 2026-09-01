package presentation.controller;

import domain.enumeration.Categorie;
import application.service.interfaces.IServiceCategorie;
import presentation.view.VueCategorie;

/*
    * ControleurCategorie enchaîne l'écran "Gérer mes catégories" : activer une catégorie,
    * en désactiver une. Chaque méthode se lit de haut en bas, sans appeler d'autre méthode
    * privée : elle demande un choix à la vue, appelle le service, transmet le résultat à la vue.
    *
    * Ni l'une ni l'autre n'a besoin de IServiceSolde : activer/désactiver une catégorie ne
    * dépend jamais du solde, contrairement aux écrans transaction et épargne. La reprise
    * après un échec de sauvegarde n'est plus gérée ici : elle est traitée une seule fois dans
    * Main, autour de l'appel à ces méthodes.
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
