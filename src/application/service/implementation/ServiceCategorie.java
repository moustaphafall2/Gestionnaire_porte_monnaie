package application.service.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
import application.service.interfaces.IServiceCategorie;
import infrastructure.persistence.CategorieRepository;

/*
    * ServiceCategorie porte les règles de gestion des catégories actives : activation,
    * désactivation, disponibilité par type.
*/
public class ServiceCategorie implements IServiceCategorie{
    private final ServicePortefeuille servicePortefeuille;
    private final CategorieRepository categorieRepository;

    public ServiceCategorie(ServicePortefeuille servicePortefeuille, CategorieRepository categorieRepository) {
        this.servicePortefeuille = servicePortefeuille;
        this.categorieRepository = categorieRepository;
    }

    boolean estActive(Categorie categorie) {
        return servicePortefeuille.getDonnees().getCategoriesActives().contains(categorie);
    }

    public Set<Categorie> getCategoriesActives() {
        return servicePortefeuille.getDonnees().getCategoriesActives();
    }

    public void activerCategorie(Categorie categorie) {
        categorieRepository.activer(categorie);
        servicePortefeuille.getDonnees().activerCategorie(categorie);
    }

    // Règle de gestion : la désactivation n'a aucun effet sur les transactions déjà enregistrées.
    public void desactiverCategorie(Categorie categorie) {
        categorieRepository.desactiver(categorie);
        servicePortefeuille.getDonnees().desactiverCategorie(categorie);
    }

    public boolean aCategorieActiveDeType(TypeTransaction type) {
        for (Categorie categorie : servicePortefeuille.getDonnees().getCategoriesActives()) {
            if (categorie.getType() == type) {
                return true;
            }
        }
        return false;
    }

    public List<Categorie> getCategoriesDisponibles() {
        List<Categorie> disponibles = new ArrayList<>();
        for (Categorie categorie : Categorie.values()) {
            if (!servicePortefeuille.getDonnees().getCategoriesActives().contains(categorie)) {
                disponibles.add(categorie);
            }
        }
        return disponibles;
    }

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
