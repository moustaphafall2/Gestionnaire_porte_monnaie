package application.dto;

import java.math.BigDecimal;
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

    private final Map<Categorie, BigDecimal> totalParCategorie;
    private final BigDecimal totalRevenus;
    private final BigDecimal totalDepenses;

    public StatistiqueDTO(Map<Categorie, BigDecimal> totalParCategorie, BigDecimal totalRevenus, BigDecimal totalDepenses) {
        this.totalParCategorie = totalParCategorie;
        this.totalRevenus = totalRevenus;
        this.totalDepenses = totalDepenses;
    }

    public Map<Categorie, BigDecimal> getTotalParCategorie() {
        return Collections.unmodifiableMap(totalParCategorie);
    }
    public BigDecimal getTotalRevenus() {
        return totalRevenus;
    }
    public BigDecimal getTotalDepenses() {
        return totalDepenses;
    }
}
