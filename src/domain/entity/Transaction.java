package domain.entity;

import java.time.LocalDate;

import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;

/*
    * Transaction représente une dépense ou un revenu. Elle ne valide rien elle-même : toute
    * construction ou modification doit passer par ServiceTransaction.
*/
public class Transaction {

    private int id;
    private double montant;
    private TypeTransaction type;
    private Categorie categorie;
    private LocalDate date;
    private String description;

    public Transaction(int id, double montant, TypeTransaction type, Categorie categorie, LocalDate date,
            String description)
    {
        this.id = id;
        this.montant = montant;
        this.type = type;
        this.categorie = categorie;
        this.date = date;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public double getMontant() {
        return montant;
    }

    public TypeTransaction getType() {
        return type;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public void setMontant(double montant) {
        this.montant = montant;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
