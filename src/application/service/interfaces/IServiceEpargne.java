package application.service.interfaces;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import application.dto.MouvementDTO;
import application.dto.ObjectifDTO;

public interface IServiceEpargne {

    public boolean depasseraCible(int idObjectif, BigDecimal montant);
    public List<ObjectifDTO> getObjectifs();
    public ObjectifDTO getObjectif(int idObjectif);
    public List<MouvementDTO> getMouvements(int idObjectif);
    public void creerObjectif(String nom, BigDecimal montantCible, LocalDate dateLimite);
    public void contribuerObjectif(int idObjectif, BigDecimal montant, LocalDate date);
    public void retirerObjectif(int idObjectif, BigDecimal montant, LocalDate date);
    public void supprimerObjectif(int idObjectif);
}
