package application.dto;

import java.time.LocalDate;

import domain.enumeration.SensMouvement;

/*
    * MouvementDTO transporte vers la présentation exactement ce qu'un écran a besoin d'afficher
    * pour un mouvement d'épargne (contribution ou retrait) : aucun calcul, aucune mise en forme,
    * uniquement des données. C'est ServiceEpargne qui le construit à partir d'un
    * MouvementEpargne du domaine ; la vue ne reçoit jamais l'entité elle-même.
*/
public class MouvementDTO {

    private final double montant;
    private final SensMouvement sens;
    private final LocalDate date;

    public MouvementDTO(double montant, SensMouvement sens, LocalDate date) {
        this.montant = montant;
        this.sens = sens;
        this.date = date;
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
}
