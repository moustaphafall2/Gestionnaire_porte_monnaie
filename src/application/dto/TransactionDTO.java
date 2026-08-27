package application.dto;

import java.time.LocalDate;

import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;

/*
    * TransactionDTO transporte vers la présentation exactement ce qu'un écran a besoin
    * d'afficher pour une transaction : aucun calcul, aucune mise en forme, uniquement des
    * données. C'est ServiceTransaction qui le construit à partir d'une Transaction du domaine ;
    * la vue ne reçoit jamais l'entité elle-même, ce qui l'empêche d'appeler un setter et de
    * modifier une transaction en contournant les règles portées par le service.
*/
public class TransactionDTO {

    private final int id;
    private final double montant;
    private final TypeTransaction type;
    private final Categorie categorie;
    private final LocalDate date;
    private final String description;

    public TransactionDTO(int id, double montant, TypeTransaction type, Categorie categorie, LocalDate date,
            String description) {
        this.id = id;
        this.montant = montant;
        this.type = type;
        this.categorie = categorie;
        this.date = date;
        this.description = description;
    }

    // Getters
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
}
