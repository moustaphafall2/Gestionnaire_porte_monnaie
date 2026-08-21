package application.service.interfaces;

import java.time.LocalDate;
import java.util.Map;

import domain.enumeration.Categorie;

public interface IServiceStatistique {
    
    public Map<Categorie, Double> getTotalParCategorie(LocalDate debut, LocalDate fin);
    public double getTotalRevenus(LocalDate debut, LocalDate fin);
    public double getTotalDepenses(LocalDate debut, LocalDate fin);
}