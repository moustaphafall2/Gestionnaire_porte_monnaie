package modele.IService;

import java.time.LocalDate;
import java.util.Map;

import modele.enumeration.Categorie;

public interface IServiceStatistique {
    
    public Map<Categorie, Double> getTotalParCategorie(LocalDate debut, LocalDate fin);
    public double[] getTotalRevenusEtDepenses(LocalDate debut, LocalDate fin);
}