package metier;

import java.util.ArrayList;
import java.util.List;

/*
    * L'énumération Categorie représente les différentes catégories de transactions financières.
    * Chaque catégorie est associée à un type de transaction (dépense ou revenu) et possède un libellé descriptif.
    * L'énumération fournit des méthodes pour accéder aux informations des catégories et pour filtrer les catégories par type de transaction.
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
    AUTRE_DEPENSE("Autre", TypeTransaction.DEPENSE),
    SALAIRE("Salaire", TypeTransaction.REVENU),
    AUTRE_REVENU("Autre", TypeTransaction.REVENU);

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

    // Méthode pour obtenir les catégories de l'enum 
    // qui correspondent au type reçu en paramètre.
    public static List<Categorie> parType(TypeTransaction type) {
        
        ArrayList<Categorie> categorieDispoType = new ArrayList<>();

        for (Categorie c : Categorie.values()) {
            if (c.getType() == type) {
                categorieDispoType.add(c);
            }
        }
        return categorieDispoType;
    }
    
}