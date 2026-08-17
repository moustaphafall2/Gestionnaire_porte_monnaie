package metier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
    * Une épargne est un objectif financier que l'utilisateur souhaite atteindre en mettant de l'argent de côté.
    * La classe Epargne est utilisée pour suivre les objectifs d'épargne de l'utilisateur et gérer les contributions et les retraits d'argent associés à chaque objectif.
    * Elle permet de fixer un montant cible à atteindre, une date limite pour atteindre cet objectif, et de suivre les mouvements d'épargne effectués par l'utilisateur.
 */
public class Epargne {
    // Les montants sont des FCFA sans centimes, mais restent des double : deux montants
    // "égaux" en théorie peuvent différer d'une poussière après une suite d'additions/
    // soustractions. On compare donc à un epsilon près plutôt qu'avec ==.
    private static final double EPSILON = 0.01;

    private int id;
    private String nom;
    private double montantCible;
    private LocalDate dateLimite;
    private List<MouvementEpargne> mouvements;

    public Epargne(int id, String nom, double montantCible, LocalDate dateLimite) {
        this.id = id;
        this.nom = nom;
        this.montantCible = montantCible;
        this.dateLimite = dateLimite;
        this.mouvements = new ArrayList<>();
    }

    // Getters
    public int getId() {
        return id;
    }
    public String getNom() {
        return nom;
    }
    public double getMontantCible() {
        return montantCible;
    }
    public LocalDate getDateLimite() {
        return dateLimite;
    }
    public List<MouvementEpargne> getMouvements() {
        return Collections.unmodifiableList(mouvements);
    }

    public double getMontantActuel() {
        double sommeContributions = 0;
        double sommeRetraits = 0;

        for (MouvementEpargne mouvement : mouvements) {
            if (mouvement.getSens() == SensMouvement.CONTRIBUTION) {
                sommeContributions += mouvement.getMontant();
            } else {
                sommeRetraits += mouvement.getMontant();
            }
        }

        return sommeContributions - sommeRetraits;
    }

    public double getPourcentageAtteint(){
        double pourcentage = (getMontantActuel() / montantCible) * 100;
        return pourcentage;
    }

    // La validation "montant ≤ solde disponible" est faite en amont,
    // dans `Portefeuille`, car `Epargne` ne connaît pas le solde global
    public void contribuer(double montant, LocalDate date){
        // Le montant et la date sont validés dans le constructeur de MouvementEpargne, donc pas besoin de le valider ici
        MouvementEpargne mouvement = new MouvementEpargne(montant, SensMouvement.CONTRIBUTION, date);
        mouvements.add(mouvement);
    }

    // Le retrait est refusé : ce n'est pas le montant en lui-même qui est invalide,
    // c'est l'état actuel de l'objectif qui ne le permet pas (IllegalStateException,
    // pas IllegalArgumentException).
    public void retirer(double montant, LocalDate date){
        if(montant > getMontantActuel()){
            throw new IllegalStateException("Le montant du retrait ne peut pas dépasser le montant actuellement épargné.");
        }

        MouvementEpargne mouvement = new MouvementEpargne(montant, SensMouvement.RETRAIT, date);
        mouvements.add(mouvement);
    }

    // Indique si une contribution de ce montant ferait dépasser le montant cible.
    // Dépasser la cible reste autorisé (règle de gestion) : c'est à l'appelant (Menu)
    // de décider s'il signale ce dépassement avant de confirmer l'opération.
    public boolean depasseraCible(double montant) {
        return getMontantActuel() + montant > montantCible;
    }

    // Condition nécessaire à la suppression de l'objectif
    public boolean estVide(){
        return Math.abs(getMontantActuel()) < EPSILON;
    }

    public boolean estAtteint(){
        if(getMontantActuel() >= montantCible){
            return true;
        }
        return false;
    }

    // Methode pour retourner une représentation lisible (ex. `"Vacances : 120 000 / 500 000 FCFA (24%)"`)
    @Override
    public String toString() {
        return nom + " : " + getMontantActuel() + " / " + montantCible + " FCFA (" + String.format("%.2f", getPourcentageAtteint()) + "%)";
    }
}
