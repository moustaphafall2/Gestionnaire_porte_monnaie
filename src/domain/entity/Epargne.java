package domain.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
    * Une épargne est un objectif financier que l'utilisateur souhaite atteindre en mettant de
    * l'argent de côté : un nom, un montant cible, une date limite facultative, et la liste des
    * mouvements (contributions et retraits) qui font évoluer le montant épargné.
    *
    * Elle ne valide plus rien elle-même : la validation (nom non vide, montant cible strictement
    * positif) est portée par ServiceEpargne. Toute construction d'une Epargne doit
    * obligatoirement passer par ce service — un appel direct au constructeur ailleurs dans le
    * code contournerait ces règles. Les calculs (montant actuel, pourcentage atteint...) et les
    * règles de gestion (contribution, retrait, suppression) sont eux aussi dans ServiceEpargne.
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

    // Remplace les anciennes méthodes contribuer()/retirer() : le mouvement est désormais
    // construit et validé par ServiceEpargne (lui seul sait si un retrait est autorisé), et
    // l'entité se contente de l'ajouter à sa liste.
    public void ajouterMouvement(MouvementEpargne mouvement) {
        mouvements.add(mouvement);
    }
}