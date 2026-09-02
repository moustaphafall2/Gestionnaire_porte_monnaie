package infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import domain.enumeration.Categorie;
import exception.ErreurChargementException;
import exception.ErreurSauvegardeException;

/*
    * CategorieRepository lit et écrit la seule table categorie_active : l'ensemble des
    * catégories que l'utilisateur a choisi d'utiliser. Chaque méthode ouvre sa propre connexion
    * via ConnexionBaseDeDonnees.ouvrir() et la referme (try-with-resources) : pas de connexion
    * partagée gardée entre deux appels.
    *
    * Pas d'interface : décision de la maîtresse de stage, pour ne pas doubler le nombre de
    * fichiers alors qu'une seule implémentation existera jamais ici. ServiceCategorie dépend
    * donc de cette classe directement.
*/
public class CategorieRepository {

    // Charge l'ensemble des catégories actives, au démarrage de l'application (voir
    // ChargeurPortefeuille). La présence d'une ligne dans la table EST l'état "active" : rien
    // d'autre à lire qu'une liste de noms de catégories.
    public Set<Categorie> chargerActives() {
        String requete = "SELECT categorie FROM categorie_active";
        Set<Categorie> actives = new HashSet<>();
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete);
                ResultSet resultat = instruction.executeQuery()) {
            while (resultat.next()) {
                actives.add(Categorie.valueOf(resultat.getString("categorie")));
            }
        } catch (SQLException exception) {
            throw new ErreurChargementException("Impossible de charger les catégories actives depuis la base de données.", exception);
        }
        return actives;
    }

    // Idempotent, comme Set.add() : activer une catégorie déjà active ne doit pas échouer.
    // ON CONFLICT DO NOTHING est le mécanisme SQL standard pour ça — sans lui, la contrainte de
    // clé primaire sur categorie_active.categorie ferait échouer une seconde activation.
    public void activer(Categorie categorie) {
        String requete = "INSERT INTO categorie_active (categorie) VALUES (?) ON CONFLICT DO NOTHING";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setString(1, categorie.name());
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible d'activer la catégorie " + categorie.getLibelle() + ".", exception);
        }
    }

    // Idempotent aussi, comme Set.remove() : désactiver une catégorie déjà inactive ne supprime
    // simplement aucune ligne, sans erreur.
    public void desactiver(Categorie categorie) {
        String requete = "DELETE FROM categorie_active WHERE categorie = ?";
        try (Connection connexion = ConnexionBaseDeDonnees.ouvrir();
                PreparedStatement instruction = connexion.prepareStatement(requete)) {
            instruction.setString(1, categorie.name());
            instruction.executeUpdate();
        } catch (SQLException exception) {
            throw new ErreurSauvegardeException("Impossible de désactiver la catégorie " + categorie.getLibelle() + ".", exception);
        }
    }
}
