package application.service.implementation;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import application.dto.StatistiqueDTO;
import domain.entity.Transaction;
import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
import application.service.interfaces.IServiceStatistique;
/*
    * ServiceStatistique calcule les statistiques du portefeuille sur une période donnée : le
    * total dépensé par catégorie, le total des revenus, le total des dépenses. Déplacé depuis
    * Portefeuille, sans changement de logique.
    *
    * Les mouvements d'épargne (contributions, retraits) ne rentrent jamais dans ce calcul :
    * la règle de gestion veut qu'ils restent stockés dans chaque Epargne, jamais dans les
    * transactions du portefeuille. Comme getStatistiques() ne parcourt que getTransactions(),
    * elle ne les voit tout simplement pas.
    *
    * Comme les autres services, il ne détient jamais Portefeuille directement : il passe par
    * servicePortefeuille.getDonnees() pour lire les transactions.
    *
    * Depuis l'étape DTO, les trois anciennes méthodes (getTotalParCategorie, getTotalRevenus,
    * getTotalDepenses) sont fusionnées en une seule, getStatistiques(), qui construit un
    * StatistiqueDTO en un seul passage sur les transactions au lieu de trois passages
    * indépendants sur la même liste.
*/
public class ServiceStatistique implements IServiceStatistique {
    private final ServicePortefeuille servicePortefeuille;

    public ServiceStatistique(ServicePortefeuille servicePortefeuille) {
        this.servicePortefeuille = servicePortefeuille;
    }

    // Une période où la date de fin précède la date de début n'a pas de sens : aucune
    // transaction ne peut jamais la satisfaire, ce n'est pas une donnée valide en soi.
    private void validerPeriode(LocalDate debut, LocalDate fin) {
        if (debut.isAfter(fin)) {
            throw new IllegalArgumentException("La date de début ne peut pas être postérieure à la date de fin.");
        }
    }

    // Statistiques complètes sur une période donnée. Un seul passage sur les transactions
    // calcule le total par catégorie, le total des revenus et le total des dépenses à la fois :
    // TypeTransaction n'a que deux valeurs (DEPENSE, REVENU), donc tout ce qui n'est pas un
    // revenu est forcément une dépense, et compte à la fois dans le total par catégorie et dans
    // le total des dépenses.
    public StatistiqueDTO getStatistiques(LocalDate debut, LocalDate fin) {
        validerPeriode(debut, fin);
        Map<Categorie, Double> totalParCategorie = new HashMap<>();
        double totalRevenus = 0;
        double totalDepenses = 0;

        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
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
