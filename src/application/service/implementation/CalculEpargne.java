package application.service.implementation;

import java.math.BigDecimal;

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
    static BigDecimal calculerMontantActuel(Epargne objectif) {
        BigDecimal sommeContributions = BigDecimal.ZERO;
        BigDecimal sommeRetraits = BigDecimal.ZERO;

        for (MouvementEpargne mouvement : objectif.getMouvements()) {
            if (mouvement.getSens() == SensMouvement.CONTRIBUTION) {
                sommeContributions = sommeContributions.add(mouvement.getMontant());
            } else {
                sommeRetraits = sommeRetraits.add(mouvement.getMontant());
            }
        }

        return sommeContributions.subtract(sommeRetraits);
    }
}
