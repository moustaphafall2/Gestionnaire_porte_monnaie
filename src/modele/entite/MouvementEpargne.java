package modele.entite;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import modele.enumeration.SensMouvement;
/*
    * La classe MouvementEpargne représente un mouvement d'épargne dans le système.
    * Chaque mouvement d'épargne a un montant, un sens (contribution ou retrait), et une date.
    * La classe est utilisée pour suivre les mouvements d'épargne effectués par l'utilisateur, permettant ainsi de gérer les contributions et les retraits d'argent dans le système.
*/

public class MouvementEpargne {
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private double montant;
    private SensMouvement sens;
    private LocalDate date;
    
    public MouvementEpargne(double montant, SensMouvement sens, LocalDate date) 
    {
        validerMontant(montant);
        validerSens(sens);
        validerDate(date);
        
        this.montant = montant;
        this.sens = sens;
        this.date = date;
    }
    // Methodes de validation
    public void validerMontant(double montant) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif.");
        }
    }

    public void validerSens(SensMouvement sens) {
        if (sens == null) {
            throw new IllegalArgumentException("Le sens du mouvement est obligatoire.");
        }
    }

    public void validerDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("La date est obligatoire.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date ne peut pas être dans le futur.");
        }
    }

    // Getters
    public double getMontant() {
        return montant;
    }
    public SensMouvement getSens() {
        return sens;
    }
    public LocalDate getDate() {
        return date;
    }

    @Override
    public String toString() {
        String signe = (sens == SensMouvement.CONTRIBUTION) ? "+" : "-";
        String libelle = (sens == SensMouvement.CONTRIBUTION) ? "contribution" : "retrait";
        return "[" + date.format(FORMAT_DATE) + "] " + signe + montant + " FCFA (" + libelle + ")";
    }
}
