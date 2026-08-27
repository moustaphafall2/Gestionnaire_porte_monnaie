package application.service.interfaces;

import java.time.LocalDate;
import java.util.List;

import application.dto.MouvementDTO;
import application.dto.ObjectifDTO;

public interface IServiceEpargne {

    public boolean depasseraCible(int idObjectif, double montant);
    public List<ObjectifDTO> getObjectifs();
    public ObjectifDTO getObjectif(int idObjectif);
    public List<MouvementDTO> getMouvements(int idObjectif);
    public void creerObjectif(String nom, double montantCible, LocalDate dateLimite);
    public void contribuerObjectif(int idObjectif, double montant, LocalDate date);
    public void retirerObjectif(int idObjectif, double montant, LocalDate date);
    public void supprimerObjectif(int idObjectif);
}
