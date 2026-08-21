package modele.iService;

import java.time.LocalDate;
import java.util.List;

import modele.entite.Epargne;

public interface IServiceEpargne {

    public double getMontantActuel(Epargne objectif);
    public double getPourcentageAtteint(Epargne objectif);
    public boolean depasseraCible(Epargne objectif, double montant);
    public boolean estAtteint(Epargne objectif);
    public List<Epargne> getObjectifs();
    public Epargne getObjectif(int idObjectif);
    public Epargne creerObjectif(String nom, double montantCible, LocalDate dateLimite);
    public void contribuerObjectif(int idObjectif, double montant, LocalDate date);
    public void retirerObjectif(int idObjectif, double montant, LocalDate date);
    public void supprimerObjectif(int idObjectif);
}
