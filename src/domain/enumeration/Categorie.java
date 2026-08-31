package domain.enumeration;

/*
    * L'énumération Categorie représente les différentes catégories de transactions financières.
    * Chaque catégorie est associée à un type de transaction (dépense ou revenu) et possède un libellé descriptif.
    * Le filtrage des catégories par type (actives ou non) est une règle de gestion : il vit dans
    * ServiceCategorie, pas ici.
*/
public enum Categorie {
    // Ici, chaque ligne crée une "instance" de Categorie,
    // en donnant les valeurs des attributs entre parenthèses
    // Les valeurs des attributs sont ensuite stockées dans les variables libelle et type
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

    // Les attributs : chaque valeur ci-dessus va stocker les siens
    private final String libelle;
    private final TypeTransaction type;

    // Le constructeur : appelé automatiquement pour chaque ligne ci-dessus
    // Ex: pour ALIMENTATION("Alimentation", TypeTransaction.DEPENSE),
    // Java appelle Categorie("Alimentation", TypeTransaction.DEPENSE)
    Categorie(String libelle, TypeTransaction type) {
        this.libelle = libelle;
        this.type = type;
    }

    // Méthodes
    public String getLibelle() {
        return libelle;
    }

    public TypeTransaction getType() {
        return type;
    }
}