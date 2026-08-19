package modele.entite;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
    * Une épargne est un objectif financier que l'utilisateur souhaite atteindre en mettant de
    * l'argent de côté : un nom, un montant cible, une date limite facultative, et la liste des
    * mouvements (contributions et retraits) qui font évoluer le montant épargné.
    *
    * Comme les autres entités, elle ne fait que porter sa structure et valider ses propres
    * champs. Les calculs (montant actuel, pourcentage atteint...) et les règles de gestion
    * (contribution, retrait, suppression) ont été déplacés vers ServiceEpargne.
 */
public class Epargne {

    private int id;
    private String nom;
    private double montantCible;
    private LocalDate dateLimite;
    private List<MouvementEpargne> mouvements;

    public Epargne(int id, String nom, double montantCible, LocalDate dateLimite) {
        validerNom(nom);
        validerMontantCible(montantCible);

        this.id = id;
        this.nom = nom;
        this.montantCible = montantCible;
        this.dateLimite = dateLimite;
        this.mouvements = new ArrayList<>();
    }

    // Méthodes de validation
    // Elles ne font rien si la valeur est correcte, et lèvent une exception sinon.

    private void validerNom(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom de l'objectif est obligatoire.");
        }
    }

    private void validerMontantCible(double montantCible) {
        if (montantCible <= 0) {
            throw new IllegalArgumentException("Le montant cible doit être strictement positif.");
        }
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
