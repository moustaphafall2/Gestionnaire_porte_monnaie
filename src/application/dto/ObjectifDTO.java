package application.dto;

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
    private final double montantCible;
    private final double montantActuel;
    private final double pourcentageAtteint;

    public ObjectifDTO(int id, String nom, double montantCible, double montantActuel, double pourcentageAtteint) {
        this.id = id;
        this.nom = nom;
        this.montantCible = montantCible;
        this.montantActuel = montantActuel;
        this.pourcentageAtteint = pourcentageAtteint;
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
    public double getMontantActuel() {
        return montantActuel;
    }
    public double getPourcentageAtteint() {
        return pourcentageAtteint;
    }
}
