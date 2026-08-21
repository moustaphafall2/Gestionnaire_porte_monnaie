package application.service.interfaces;

import java.time.LocalDate;
import java.util.List;

import domain.entity.Epargne;

public interface IServiceEpargne {

    public double getMontantActuel(Epargne objectif);
    public double getPourcentageAtteint(Epargne objectif);
    public List<Double> getMontantsActuels(List<Epargne> objectifs);
    public List<Double> getPourcentagesAtteints(List<Epargne> objectifs);
    public boolean depasseraCible(Epargne objectif, double montant);
    public boolean estAtteint(Epargne objectif);
    public List<Epargne> getObjectifs();
    public boolean aAuMoinsUnObjectif();
    public Epargne getObjectif(int idObjectif);
    public Epargne creerObjectif(String nom, double montantCible, LocalDate dateLimite);
    public void contribuerObjectif(int idObjectif, double montant, LocalDate date);
    public void retirerObjectif(int idObjectif, double montant, LocalDate date);
    public void supprimerObjectif(int idObjectif);
}
