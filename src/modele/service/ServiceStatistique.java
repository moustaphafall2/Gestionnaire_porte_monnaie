package modele.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import modele.entite.Transaction;
import modele.enumeration.Categorie;
import modele.enumeration.TypeTransaction;
import modele.iService.IServiceStatistique;
/*
    * ServiceStatistique calcule les statistiques du portefeuille sur une période donnée : le
    * total dépensé par catégorie, et le total des revenus et des dépenses. Déplacé depuis
    * Portefeuille, sans changement de logique.
    *
    * Les mouvements d'épargne (contributions, retraits) ne rentrent jamais dans ces calculs :
    * la règle de gestion veut qu'ils restent stockés dans chaque Epargne, jamais dans les
    * transactions du portefeuille. Comme les deux méthodes ci-dessous ne parcourent que
    * getTransactions(), elles ne les voient tout simplement pas.
    *
    * Comme les autres services, il ne détient jamais Portefeuille directement : il passe par
    * servicePortefeuille.getDonnees() pour lire les transactions.
*/
public class ServiceStatistique implements IServiceStatistique {
    private ServicePortefeuille servicePortefeuille;

    public ServiceStatistique(ServicePortefeuille servicePortefeuille) {
        this.servicePortefeuille = servicePortefeuille;
    }

    // Total dépensé par catégorie, sur une période donnée
    public Map<Categorie, Double> getTotalParCategorie(LocalDate debut, LocalDate fin) {
        Map<Categorie, Double> totaux = new HashMap<>();

        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            LocalDate date = transaction.getDate();
            if (date.isBefore(debut) || date.isAfter(fin)) {
                continue;
            }
            if (transaction.getType() == TypeTransaction.DEPENSE) {
                Categorie categorie = transaction.getCategorie();
                double totalActuel = totaux.getOrDefault(categorie, 0.0);
                totaux.put(categorie, totalActuel + transaction.getMontant());
            }
        }

        return totaux;
    }

    // Total des revenus sur une période donnée. Séparée de getTotalDepenses() plutôt que de
    // renvoyer les deux dans un tableau : le contrôleur recevrait alors deux valeurs indexées
    // (totaux[0], totaux[1]) qu'il devrait lui-même extraire, ce qui est une manipulation de
    // donnée qui n'a rien à faire dans un contrôleur.
    public double getTotalRevenus(LocalDate debut, LocalDate fin) {
        double totalRevenus = 0;

        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            LocalDate date = transaction.getDate();
            if (date.isBefore(debut) || date.isAfter(fin)) {
                continue;
            }
            if (transaction.getType() == TypeTransaction.REVENU) {
                totalRevenus += transaction.getMontant();
            }
        }

        return totalRevenus;
    }

    // Symétrique de getTotalRevenus(), pour les dépenses.
    public double getTotalDepenses(LocalDate debut, LocalDate fin) {
        double totalDepenses = 0;

        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            LocalDate date = transaction.getDate();
            if (date.isBefore(debut) || date.isAfter(fin)) {
                continue;
            }
            if (transaction.getType() == TypeTransaction.DEPENSE) {
                totalDepenses += transaction.getMontant();
            }
        }

        return totalDepenses;
    }
}
