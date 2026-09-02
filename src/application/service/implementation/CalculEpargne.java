package application.service.implementation;

import domain.entity.Epargne;
import domain.entity.MouvementEpargne;
import domain.enumeration.SensMouvement;

/*
    * CalculEpargne isole le calcul du montant actuellement épargné sur un objectif, partagé par
    * ServiceEpargne et ServiceSolde pour n'exister qu'à un seul endroit.
*/
public class CalculEpargne {

    private CalculEpargne() {
    }

    // Règle de gestion : jamais stocké, toujours recalculé à partir des mouvements.
    static double calculerMontantActuel(Epargne objectif) {
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
