import domain.entity.Portefeuille;
import exception.ErreurChargementException;
import infrastructure.persistence.GestionnairePostgreSQL;

/*
    * Programme de vérification, à lancer une seule fois pour confirmer que charger()
    * reconstruit correctement un Portefeuille depuis la base de données, avant d'écrire les
    * méthodes d'écriture. Comme TesterConnexion : hors architecture, à supprimer une fois validé.
*/
public class TesterChargement {
    public static void main(String[] args) {
        try {
            Portefeuille portefeuille = new GestionnairePostgreSQL().charger();
            System.out.println("Transactions chargées : " + portefeuille.getTransactions().size());
            System.out.println("Catégories actives : " + portefeuille.getCategoriesActives().size());
            System.out.println("Objectifs d'épargne : " + portefeuille.getObjectifs().size());
        } catch (ErreurChargementException exception) {
            System.out.println("Échec du chargement : " + exception.getMessage());
        }
    }
}
