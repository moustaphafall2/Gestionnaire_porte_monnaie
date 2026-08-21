package modele.iService;

import java.time.LocalDate;
import java.util.List;

import modele.entite.Transaction;
import modele.enumeration.Categorie;
import modele.enumeration.TypeTransaction;

public interface IServiceTransaction {
    
    public Transaction ajouterDepense(double montant, Categorie categorie, LocalDate date, String description);
    public Transaction ajouterRevenu(double montant, Categorie categorie, LocalDate date, String description);
    public void modifierTransaction(int id, double nouveauMontant, Categorie nouvelleCategorie, LocalDate nouvelleDate, String nouvelleDescription);
    public void supprimerTransaction(int id);
    public Transaction getTransaction(int id);
    public List<Transaction> getHistorique();
    public List<Transaction> filtrerParDate(LocalDate debut, LocalDate fin);
    public List<Transaction> filtrerParCategorie(Categorie categorie);
    public List<Transaction> filtrerParType(TypeTransaction type);
}