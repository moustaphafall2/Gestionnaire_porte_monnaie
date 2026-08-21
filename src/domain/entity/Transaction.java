package domain.entity;

import java.time.LocalDate;

import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
/*
    * La classe Transaction représente une transaction financière,
    * qui peut être soit une dépense, soit un revenu.
    * Chaque transaction a un identifiant unique, un montant, un type (dépense ou revenu), une catégorie, une date et une description.
    * La classe fournit des méthodes pour accéder, valider et modifier ces informations, ainsi qu'une méthode pour afficher les détails de la transaction sous forme de
    * chaîne de caractères.
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

        validerId(id);
        validerType(type);
        validerCategorie(categorie, type);
        validerMontant(montant);
        validerDate(date);
        
        this.id = id;
        this.montant = montant;
        this.type = type;
        this.categorie = categorie;
        this.date = date;
        this.description = description;

    }

    // Méthodes de validation
    // Elles ne font rien si la valeur est correcte,
    // et lèvent une exception sinon. Réutilisées par le constructeur et les setters.

    private void validerId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'identifiant doit être strictement positif.");
        }
    }

    private void validerMontant(double montant) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif.");
        }
    }

    private void validerType(TypeTransaction type) {
        if (type == null) {
            throw new IllegalArgumentException("Le type de transaction est obligatoire.");
        }
    }

    private void validerCategorie(Categorie categorie, TypeTransaction type) {
        if (categorie == null) {
            throw new IllegalArgumentException("La catégorie est obligatoire.");
        }
        if (categorie.getType() != type) {
            throw new IllegalArgumentException("La catégorie ne correspond pas au type de la transaction.");
        }
    }

    private void validerDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("La date est obligatoire.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date ne peut pas être dans le futur.");
        }
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
        validerMontant(montant);
        this.montant = montant;
    }

    public void setCategorie(Categorie categorie) {
        validerCategorie(categorie, this.type);
        this.categorie = categorie;
    }

    public void setDate(LocalDate date) {
        validerDate(date);
        this.date = date;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    // Méthode toString() pour afficher les informations d'une transaction
    // sous forme de chaîne de caractères.
    @Override
    public String toString() {
        String descriptionAffichee = (getDescription() == null || getDescription().isBlank()) ? "(sans description)" : getDescription();
        return getDate() + " - Transaction " + getId() + " " + getCategorie().getLibelle()
                + " - " + descriptionAffichee + ", montant = " + getMontant() + ", type = " + getType();
    }
}
