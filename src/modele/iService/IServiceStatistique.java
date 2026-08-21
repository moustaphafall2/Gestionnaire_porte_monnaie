package modele.iService;

import java.time.LocalDate;
import java.util.Map;

import modele.enumeration.Categorie;

public interface IServiceStatistique {
    
    public Map<Categorie, Double> getTotalParCategorie(LocalDate debut, LocalDate fin);
    public double getTotalRevenus(LocalDate debut, LocalDate fin);
    public double getTotalDepenses(LocalDate debut, LocalDate fin);
}