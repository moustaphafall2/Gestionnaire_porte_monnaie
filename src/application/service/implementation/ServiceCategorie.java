package application.service.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
import application.service.interfaces.IServiceCategorie;

/*
    * ServiceCategorie porte les règles de gestion des catégories actives : savoir si une
    * catégorie est active, si un type de transaction a au moins une catégorie active, quelles
    * catégories restent disponibles à l'activation, et l'activation/désactivation elles-mêmes.
    * `Portefeuille.activerCategorie`/`desactiverCategorie` restent des méthodes structurelles
    * (ajout/retrait dans un Set), mais déclencher la sauvegarde après coup est une
    * responsabilité du service, jamais de l'entité.
*/
public class ServiceCategorie implements IServiceCategorie{
    private final ServicePortefeuille servicePortefeuille;

    public ServiceCategorie(ServicePortefeuille servicePortefeuille) {
        this.servicePortefeuille = servicePortefeuille;
    }

    boolean estActive(Categorie categorie) {
        return servicePortefeuille.getDonnees().getCategoriesActives().contains(categorie);
    }

    // Catégories actuellement actives, utilisée par ControleurCategorie pour l'affichage.
    public Set<Categorie> getCategoriesActives() {
        return servicePortefeuille.getDonnees().getCategoriesActives();
    }

    public void activerCategorie(Categorie categorie) {
        servicePortefeuille.getDonnees().activerCategorie(categorie);
        servicePortefeuille.sauvegarder();
    }

    // Règle de gestion : la désactivation n'a aucun effet sur les transactions déjà
    // enregistrées avec cette catégorie. Portefeuille.desactiverCategorie ne fait que la
    // retirer de l'ensemble des catégories actives, jamais des transactions elles-mêmes.
    public void desactiverCategorie(Categorie categorie) {
        servicePortefeuille.getDonnees().desactiverCategorie(categorie);
        servicePortefeuille.sauvegarder();
    }

    // Indique si au moins une catégorie active correspond à ce type. Utilisé par
    // ControleurTransaction pour vérifier la précondition avant de proposer d'ajouter une
    // dépense/un revenu.
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

    // Catégories actives qui correspondent à ce type. Utilisée par ControleurTransaction pour
    // proposer, à l'ajout d'une dépense ou d'un revenu, uniquement les catégories du bon type
    // (règle de gestion "catégorie cohérente").
    public List<Categorie> getCategoriesActivesDeType(TypeTransaction type) {
        List<Categorie> resultat = new ArrayList<>();
        for (Categorie categorie : servicePortefeuille.getDonnees().getCategoriesActives()) {
            if (categorie.getType() == type) {
                resultat.add(categorie);
            }
        }
        return resultat;
    }
}
