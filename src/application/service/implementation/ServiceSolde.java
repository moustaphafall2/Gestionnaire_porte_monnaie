package application.service.implementation;

import java.math.BigDecimal;

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
    public BigDecimal getSoldeDisponible() {
        BigDecimal totalRevenus = BigDecimal.ZERO;
        BigDecimal totalDepenses = BigDecimal.ZERO;

        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            if (transaction.getType() == TypeTransaction.REVENU) {
                totalRevenus = totalRevenus.add(transaction.getMontant());
            } else {
                totalDepenses = totalDepenses.add(transaction.getMontant());
            }
        }

        return totalRevenus.subtract(totalDepenses).subtract(getTotalEpargne());
    }

    public BigDecimal getTotalEpargne() {
        BigDecimal total = BigDecimal.ZERO;
        for (Epargne objectif : servicePortefeuille.getDonnees().getObjectifs()) {
            total = total.add(CalculEpargne.calculerMontantActuel(objectif));
        }
        return total;
    }

    public BigDecimal soldeApresDepense(BigDecimal montant) {
        return getSoldeDisponible().subtract(montant);
    }

    public boolean depenseRendraSoldeNegatif(BigDecimal montant) {
        return soldeApresDepense(montant).compareTo(BigDecimal.ZERO) < 0;
    }
}
