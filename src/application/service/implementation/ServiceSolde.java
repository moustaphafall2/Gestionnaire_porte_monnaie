package application.service.implementation;

import domain.entity.Epargne;
import domain.entity.Transaction;
import domain.enumeration.TypeTransaction;
import application.service.interfaces.IServiceSolde;

/*
    * ServiceSolde calcule le solde disponible et le total épargné, recalculés à chaque appel,
    * jamais stockés. Dépend de ServicePortefeuille (classe concrète, pas d'interface) car
    * getDonnees() est à visibilité de paquet.
*/
public class ServiceSolde implements IServiceSolde {
    private final ServicePortefeuille servicePortefeuille;

    public ServiceSolde(ServicePortefeuille servicePortefeuille) {
        this.servicePortefeuille = servicePortefeuille;
    }

    // Règle de gestion : solde disponible = total des revenus - total des dépenses - total
    // actuellement épargné.
    public double getSoldeDisponible() {
        double totalRevenus = 0;
        double totalDepenses = 0;

        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            if (transaction.getType() == TypeTransaction.REVENU) {
                totalRevenus += transaction.getMontant();
            } else {
                totalDepenses += transaction.getMontant();
            }
        }

        return totalRevenus - totalDepenses - getTotalEpargne();
    }

    public double getTotalEpargne() {
        double total = 0;
        for (Epargne objectif : servicePortefeuille.getDonnees().getObjectifs()) {
            total += CalculEpargne.calculerMontantActuel(objectif);
        }
        return total;
    }

    public double soldeApresDepense(double montant) {
        return getSoldeDisponible() - montant;
    }

    public boolean depenseRendraSoldeNegatif(double montant) {
        return soldeApresDepense(montant) < 0;
    }
}
