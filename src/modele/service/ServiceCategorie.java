package modele.service;

import java.util.ArrayList;
import java.util.List;

import modele.enumeration.Categorie;
import modele.enumeration.TypeTransaction;

/*
    * ServiceCategorie porte les règles de gestion des catégories actives : savoir si une
    * catégorie est active, si un type de transaction a au moins une catégorie active, et
    * quelles catégories restent disponibles à l'activation. `activerCategorie` et
    * `desactiverCategorie` restent pour l'instant dans Portefeuille, en attendant de migrer
    * ici à leur tour.
*/
public class ServiceCategorie {
    private ServicePortefeuille servicePortefeuille;

    public ServiceCategorie(ServicePortefeuille servicePortefeuille) {
        this.servicePortefeuille = servicePortefeuille;
    }

    public boolean estActive(Categorie categorie) {
        return servicePortefeuille.getDonnees().getCategoriesActives().contains(categorie);
    }

    // Indique si au moins une catégorie active correspond à ce type. Utilisé par Menu pour
    // vérifier la précondition avant de proposer d'ajouter une dépense/un revenu.
    public boolean aCategorieActiveDeType(TypeTransaction type) {
        for (Categorie categorie : servicePortefeuille.getDonnees().getCategoriesActives()) {
            if (categorie.getType() == type) {
                return true;
            }
        }
        return false;
    }

    // Catégories de la liste complète (l'énumération) qui ne sont pas encore actives
    public List<Categorie> getCategoriesDisponibles() {
        List<Categorie> disponibles = new ArrayList<>();
        for (Categorie categorie : Categorie.values()) {
            if (!servicePortefeuille.getDonnees().getCategoriesActives().contains(categorie)) {
                disponibles.add(categorie);
            }
        }
        return disponibles;
    }
}
