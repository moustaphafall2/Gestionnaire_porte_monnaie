package domain.entity;

import java.time.LocalDate;

import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
/*
    * La classe Transaction représente une transaction financière, dépense ou revenu.
    * Chaque transaction a un identifiant, un montant, un type, une catégorie, une date et une
    * description.
    *
    * Elle ne valide plus rien elle-même : toute la validation (montant strictement positif,
    * date jamais postérieure au jour, catégorie du même type que la transaction, description
    * jamais nulle) est portée par ServiceTransaction. Toute construction ou modification d'une
    * Transaction doit obligatoirement passer par ce service — un appel direct au constructeur ou
    * à un setter ailleurs dans le code contournerait ces règles.
 */
public class Transaction {

    // Attributs des transactions. Ils decrivent les informations
    // que l'on souhaite stocker pour chaque transaction.
    private int id;
    private double montant;
    private TypeTransaction type;
    private Categorie categorie;
    private LocalDate date;
    private String description;

    // Constructeur de la classe Transaction. Il permet de créer une nouvelle transaction
    // en initialisant les attributs avec les valeurs passées en paramètre.
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

    // Méthodes "getter" et "setter" pour chaque attribut.
    // Elles permettent de récupérer ou de modifier
    // les valeurs des attributs d'une transaction.
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