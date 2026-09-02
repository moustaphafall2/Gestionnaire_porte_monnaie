package domain.entity;

import java.time.LocalDate;

import domain.enumeration.SensMouvement;

/*
    * MouvementEpargne représente une contribution ou un retrait sur un objectif d'épargne. Ne
    * valide rien elle-même : toute construction doit passer par ServiceEpargne.
*/
public class MouvementEpargne {

    private double montant;
    private SensMouvement sens;
    private LocalDate date;

    public MouvementEpargne(double montant, SensMouvement sens, LocalDate date)
    {
        this.montant = montant;
        this.sens = sens;
        this.date = date;
    }

    public double getMontant() {
        return montant;
    }
    public SensMouvement getSens() {
        return sens;
    }
    public LocalDate getDate() {
        return date;
    }
}
