package application.service.interfaces;

import java.time.LocalDate;
import java.util.List;

import application.dto.TransactionDTO;
import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;

public interface IServiceTransaction {

    public void ajouterDepense(double montant, Categorie categorie, LocalDate date, String description);
    public void ajouterRevenu(double montant, Categorie categorie, LocalDate date, String description);
    public void modifierTransaction(int id, double nouveauMontant, Categorie nouvelleCategorie, LocalDate nouvelleDate, String nouvelleDescription);
    public void supprimerTransaction(int id);
    public TransactionDTO getTransaction(int id);
    public List<TransactionDTO> getHistorique();
    public List<TransactionDTO> filtrerParDate(LocalDate debut, LocalDate fin);
    public List<TransactionDTO> filtrerParCategorie(Categorie categorie);
    public List<TransactionDTO> filtrerParType(TypeTransaction type);
}