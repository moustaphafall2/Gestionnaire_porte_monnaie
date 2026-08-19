package modele.service;

import java.time.LocalDate;

import modele.entite.Epargne;

/*
    * ServiceEpargne porte les règles de gestion des objectifs d'épargne. Il ne contient pour
    * l'instant qu'une seule méthode (contribuerObjectif), déplacée depuis Portefeuille. Les
    * autres opérations sur les objectifs (retirerObjectif, supprimerObjectif, creerObjectif)
    * migreront ici au fur et à mesure.
    *
    * Il ne détient pas Portefeuille directement : seul ServicePortefeuille a cette référence.
    * Ici, on passe par getDonnees() (accessible uniquement depuis modele.service) pour lire
    * l'objectif concerné, et par sauvegarder() pour écrire le résultat.
*/
public class ServiceEpargne {
    private ServicePortefeuille servicePortefeuille;

    public ServiceEpargne(ServicePortefeuille servicePortefeuille) {
        this.servicePortefeuille = servicePortefeuille;
    }

    // Un objectif d'épargne fonctionne comme un coffre : impossible d'y placer une somme
    // dont on ne dispose pas. La vérification se fait ici, et pas dans Epargne, car Epargne
    // ne connaît pas le solde disponible du portefeuille. Refusé à cause de l'état actuel
    // du portefeuille (pas d'une donnée invalide en soi) : IllegalStateException.
    public void contribuerObjectif(int idObjectif, double montant, LocalDate date) {
        if (montant > servicePortefeuille.getSoldeDisponible()) {
            throw new IllegalStateException("Le montant de la contribution dépasse le solde disponible.");
        }

        Epargne objectif = servicePortefeuille.getDonnees().getObjectif(idObjectif);
        objectif.contribuer(montant, date);
        servicePortefeuille.sauvegarder();
    }
}
