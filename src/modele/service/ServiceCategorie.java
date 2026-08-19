package modele.service;

import modele.enumeration.Categorie;

/*
    * ServiceCategorie porte les règles de gestion des catégories actives. Il ne contient pour
    * l'instant qu'une seule méthode, estActive() : ServiceTransaction en a besoin pour
    * appliquer la règle "une transaction ne peut porter qu'une catégorie active", et cette
    * règle ne pouvait pas rester dans Portefeuille en attendant que ServiceCategorie existe.
    * Les autres opérations (activerCategorie, desactiverCategorie, getCategoriesDisponibles...)
    * migreront ici au fur et à mesure.
*/
public class ServiceCategorie {
    private ServicePortefeuille servicePortefeuille;

    public ServiceCategorie(ServicePortefeuille servicePortefeuille) {
        this.servicePortefeuille = servicePortefeuille;
    }

    public boolean estActive(Categorie categorie) {
        return servicePortefeuille.getDonnees().getCategoriesActives().contains(categorie);
    }
}
