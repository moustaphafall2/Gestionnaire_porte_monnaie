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
    * ServiceStatistique calcule les statistiques du portefeuille sur une période donnée : le
    * total dépensé par catégorie, le total des revenus, le total des dépenses.
    *
    * Les mouvements d'épargne (contributions, retraits) ne rentrent jamais dans ce calcul :
    * la règle de gestion veut qu'ils restent stockés dans chaque Epargne, jamais dans les
    * transactions du portefeuille. Comme getStatistiques() ne parcourt que l'historique des
    * transactions, elle ne les voit tout simplement pas.
    *
    * Depuis l'étape 5, ce service ne dépend plus de ServicePortefeuille : en lecture seule, il
    * n'a jamais eu besoin que des transactions, déjà exposées publiquement par
    * IServiceTransaction.getHistorique() (qui renvoie des TransactionDTO, aux mêmes champs
    * qu'une Transaction). Passer par cette interface plutôt que par
    * ServicePortefeuille.getDonnees() élimine sa dépendance au pivot, et devient une dépendance
    * déclarée par interface, comme le veut la règle du projet — ce que ServicePortefeuille ne
    * pouvait pas offrir (getDonnees() est à visibilité de paquet, donc jamais dans une
    * interface).
    *
    * Un service qui consomme le DTO d'un autre service : arbitrage assumé, pas un oubli — voir
    * le journal de développement pour le raisonnement complet.
*/
public class ServiceStatistique implements IServiceStatistique {
    private final IServiceTransaction serviceTransaction;

    public ServiceStatistique(IServiceTransaction serviceTransaction) {
        this.serviceTransaction = serviceTransaction;
    }

    // Une période où la date de fin précède la date de début n'a pas de sens : aucune
    // transaction ne peut jamais la satisfaire, ce n'est pas une donnée valide en soi.
    private void validerPeriode(LocalDate debut, LocalDate fin) {
        if (debut.isAfter(fin)) {
            throw new IllegalArgumentException("La date de début ne peut pas être postérieure à la date de fin.");
        }
    }

    // Statistiques complètes sur une période donnée. Un seul passage sur l'historique calcule
    // le total par catégorie, le total des revenus et le total des dépenses à la fois :
    // TypeTransaction n'a que deux valeurs (DEPENSE, REVENU), donc tout ce qui n'est pas un
    // revenu est forcément une dépense, et compte à la fois dans le total par catégorie et dans
    // le total des dépenses.
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
