package application.dto;

import java.util.Collections;
import java.util.Map;

import domain.enumeration.Categorie;

/*
    * StatistiqueDTO transporte vers la présentation exactement ce que l'écran statistiques a
    * besoin d'afficher pour une période donnée : le total dépensé par catégorie, le total des
    * revenus, le total des dépenses. Aucun calcul, aucune mise en forme : c'est
    * ServiceStatistique qui construit ce DTO à partir des transactions du domaine, en un seul
    * passage sur la liste.
*/
public class StatistiqueDTO {

    private final Map<Categorie, Double> totalParCategorie;
    private final double totalRevenus;
    private final double totalDepenses;

    public StatistiqueDTO(Map<Categorie, Double> totalParCategorie, double totalRevenus, double totalDepenses) {
        this.totalParCategorie = totalParCategorie;
        this.totalRevenus = totalRevenus;
        this.totalDepenses = totalDepenses;
    }

    public Map<Categorie, Double> getTotalParCategorie() {
        return Collections.unmodifiableMap(totalParCategorie);
    }
    public double getTotalRevenus() {
        return totalRevenus;
    }
    public double getTotalDepenses() {
        return totalDepenses;
    }
}
