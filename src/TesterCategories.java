import domain.enumeration.Categorie;
import infrastructure.persistence.GestionnairePostgreSQL;

/*
    * Programme de vérification, à lancer une seule fois pour confirmer qu'activerCategorie()/
    * desactiverCategorie() écrivent correctement dans categorie_active, avant de brancher
    * PortefeuilleRepository dans l'application. Comme TesterConnexion/TesterChargement : hors
    * architecture, à supprimer une fois validé.
    *
    * Ne teste rien tout seul : après chaque étape, vérifiez dans pgAdmin que la table
    * categorie_active contient bien ce qui est annoncé ci-dessous.
*/
public class TesterCategories {
    public static void main(String[] args) {
        GestionnairePostgreSQL repository = new GestionnairePostgreSQL();

        System.out.println("Activation de ALIMENTATION — vérifiez : une ligne 'ALIMENTATION' dans categorie_active.");
        repository.activerCategorie(Categorie.ALIMENTATION);

        System.out.println("Activation de ALIMENTATION une seconde fois — vérifiez : toujours une seule ligne, aucune erreur.");
        repository.activerCategorie(Categorie.ALIMENTATION);

        System.out.println("Activation de TRANSPORT — vérifiez : une deuxième ligne, 'TRANSPORT'.");
        repository.activerCategorie(Categorie.TRANSPORT);

        System.out.println("Désactivation de ALIMENTATION — vérifiez : la ligne 'ALIMENTATION' a disparu, 'TRANSPORT' reste.");
        repository.desactiverCategorie(Categorie.ALIMENTATION);

        System.out.println("Désactivation de ALIMENTATION une seconde fois — vérifiez : aucune erreur, rien à supprimer.");
        repository.desactiverCategorie(Categorie.ALIMENTATION);

        System.out.println("Terminé.");
    }
}
