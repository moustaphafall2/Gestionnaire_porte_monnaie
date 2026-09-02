package application.service.implementation;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import application.dto.StatistiqueDTO;
import application.dto.TransactionDTO;
import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
import application.service.interfaces.IServiceStatistique;
import application.service.interfaces.IServiceTransaction;

/*
    * ServiceStatistique calcule les statistiques du portefeuille sur une période donnée : total
    * dépensé par catégorie, total des revenus, total des dépenses. Les mouvements d'épargne
    * n'entrent jamais dans ce calcul, ils ne font pas partie de l'historique des transactions.
*/
public class ServiceStatistique implements IServiceStatistique {
    private final IServiceTransaction serviceTransaction;

    public ServiceStatistique(IServiceTransaction serviceTransaction) {
        this.serviceTransaction = serviceTransaction;
    }

    private void validerPeriode(LocalDate debut, LocalDate fin) {
        if (debut.isAfter(fin)) {
            throw new IllegalArgumentException("La date de début ne peut pas être postérieure à la date de fin.");
        }
    }

    public StatistiqueDTO getStatistiques(LocalDate debut, LocalDate fin) {
        validerPeriode(debut, fin);
        Map<Categorie, Double> totalParCategorie = new HashMap<>();
        double totalRevenus = 0;
        double totalDepenses = 0;

        for (TransactionDTO transaction : serviceTransaction.getHistorique()) {
            LocalDate date = transaction.getDate();
            if (date.isBefore(debut) || date.isAfter(fin)) {
                continue;
            }
            if (transaction.getType() == TypeTransaction.REVENU) {
                totalRevenus += transaction.getMontant();
            } else {
                Categorie categorie = transaction.getCategorie();
                double totalActuel = totalParCategorie.getOrDefault(categorie, 0.0);
                totalParCategorie.put(categorie, totalActuel + transaction.getMontant());
                totalDepenses += transaction.getMontant();
            }
        }

        return new StatistiqueDTO(totalParCategorie, totalRevenus, totalDepenses);
    }
}
