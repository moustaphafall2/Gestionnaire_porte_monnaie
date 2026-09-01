import java.time.LocalDate;

import domain.enumeration.SensMouvement;
import infrastructure.persistence.GestionnairePostgreSQL;

/*
    * Programme de vérification, à lancer une seule fois pour confirmer qu'ajouterObjectif()/
    * ajouterMouvement()/supprimerObjectif() écrivent correctement dans epargne et
    * mouvement_epargne, avant de brancher PortefeuilleRepository dans l'application. Comme les
    * précédents : hors architecture, à supprimer une fois validé.
*/
public class TesterEpargne {
    public static void main(String[] args) {
        GestionnairePostgreSQL repository = new GestionnairePostgreSQL();

        int id = repository.ajouterObjectif("Voyage", 200000, null);
        System.out.println("Objectif ajouté, id = " + id
                + " — vérifiez : une ligne dans epargne, nom 'Voyage', montant_cible 200000, date_limite NULL.");

        repository.ajouterMouvement(id, 50000, SensMouvement.CONTRIBUTION, LocalDate.now());
        System.out.println("Contribution ajoutée — vérifiez : une ligne dans mouvement_epargne, objectif_id = " + id
                + ", montant 50000, sens CONTRIBUTION.");

        repository.ajouterMouvement(id, 20000, SensMouvement.RETRAIT, LocalDate.now());
        System.out.println("Retrait ajouté — vérifiez : une deuxième ligne, montant 20000, sens RETRAIT.");

        repository.supprimerObjectif(id);
        System.out.println("Objectif " + id + " supprimé — vérifiez : la ligne a disparu de epargne, "
                + "ET les deux lignes de mouvement_epargne ont disparu avec elle (ON DELETE CASCADE).");

        System.out.println("Terminé.");
    }
}
