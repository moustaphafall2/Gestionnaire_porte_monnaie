package application.dto;

import java.math.BigDecimal;

/*
    * ObjectifDTO transporte vers la présentation exactement ce qu'un écran a besoin d'afficher
    * pour un objectif d'épargne : aucun calcul, aucune mise en forme, uniquement des données.
    * C'est ServiceEpargne qui le construit à partir d'une Epargne du domaine, montantActuel et
    * pourcentageAtteint déjà calculés inclus — la vue ne reçoit jamais l'entité elle-même, et ne
    * refait aucun calcul.
*/
public class ObjectifDTO {

    private final int id;
    private final String nom;
    private final BigDecimal montantCible;
    private final BigDecimal montantActuel;
    private final double pourcentageAtteint;

    public ObjectifDTO(int id, String nom, BigDecimal montantCible, BigDecimal montantActuel, double pourcentageAtteint) {
        this.id = id;
        this.nom = nom;
        this.montantCible = montantCible;
        this.montantActuel = montantActuel;
        this.pourcentageAtteint = pourcentageAtteint;
    }

    public int getId() {
        return id;
    }
    public String getNom() {
        return nom;
    }
    public BigDecimal getMontantCible() {
        return montantCible;
    }
    public BigDecimal getMontantActuel() {
        return montantActuel;
    }
    public double getPourcentageAtteint() {
        return pourcentageAtteint;
    }
}
