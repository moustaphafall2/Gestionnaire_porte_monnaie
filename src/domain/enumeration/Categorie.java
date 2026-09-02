package domain.enumeration;

/*
    * Categorie associe à chaque catégorie son libellé affiché et le type de transaction auquel
    * elle s'applique. Le filtrage par type actif vit dans ServiceCategorie, pas ici.
*/
public enum Categorie {
    ALIMENTATION("Alimentation", TypeTransaction.DEPENSE),
    TRANSPORT("Transport", TypeTransaction.DEPENSE),
    LOGEMENT("Logement", TypeTransaction.DEPENSE),
    LOISIRS("Loisirs", TypeTransaction.DEPENSE),
    SANTE("Santé", TypeTransaction.DEPENSE),
    ABONNEMENTS("Abonnements", TypeTransaction.DEPENSE),
    ENTRETIEN_VESTIMENTAIRE("Entretien vestimentaire", TypeTransaction.DEPENSE),
    COUTURE("Coûture", TypeTransaction.DEPENSE),
    AUTRE_DEPENSE("Autre dépense", TypeTransaction.DEPENSE),
    SALAIRE("Salaire", TypeTransaction.REVENU),
    BOURSE("Bourse", TypeTransaction.REVENU),
    AUTRE_REVENU("Autre revenu", TypeTransaction.REVENU);

    private final String libelle;
    private final TypeTransaction type;

    Categorie(String libelle, TypeTransaction type) {
        this.libelle = libelle;
        this.type = type;
    }

    public String getLibelle() {
        return libelle;
    }

    public TypeTransaction getType() {
        return type;
    }
}
