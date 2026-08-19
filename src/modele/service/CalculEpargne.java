package modele.service;

import modele.entite.Epargne;
import modele.entite.MouvementEpargne;
import modele.enumeration.SensMouvement;

/*
    * CalculEpargne rassemble le seul calcul qui doit exister à un seul endroit : le montant
    * actuellement épargné sur un objectif, recalculé à partir de ses mouvements. Deux services
    * en ont besoin — ServiceEpargne (pour ses propres règles) et ServicePortefeuille (pour le
    * total épargné et le solde disponible) — et aucun des deux ne peut dépendre de l'autre sans
    * créer un cycle (ServiceEpargne dépend déjà de ServicePortefeuille). Une classe utilitaire
    * sans état et sans dépendance résout le problème : les deux services l'appellent, elle
    * n'appelle personne.
*/
public class CalculEpargne {

    // Classe utilitaire : uniquement une méthode statique, pas d'instance à créer.
    private CalculEpargne() {
    }

    // Montant actuellement épargné sur cet objectif. Règle de gestion : jamais stocké, toujours
    // recalculé à partir des mouvements (somme des contributions moins somme des retraits).
    public static double calculerMontantActuel(Epargne objectif) {
        double sommeContributions = 0;
        double sommeRetraits = 0;

        for (MouvementEpargne mouvement : objectif.getMouvements()) {
            if (mouvement.getSens() == SensMouvement.CONTRIBUTION) {
                sommeContributions += mouvement.getMontant();
            } else {
                sommeRetraits += mouvement.getMontant();
            }
        }

        return sommeContributions - sommeRetraits;
    }
}
