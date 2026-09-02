package domain.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
    * Epargne est un objectif d'épargne : nom, montant cible, date limite facultative, et la
    * liste des mouvements qui font évoluer le montant épargné. Ne valide rien elle-même : toute
    * construction doit passer par ServiceEpargne.
*/
public class Epargne {

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

    public void ajouterMouvement(MouvementEpargne mouvement) {
        mouvements.add(mouvement);
    }
}
