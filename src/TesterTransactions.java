import java.time.LocalDate;

import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
import infrastructure.persistence.GestionnairePostgreSQL;

/*
    * Programme de vérification, à lancer une seule fois pour confirmer qu'ajouterTransaction()/
    * modifierTransaction()/supprimerTransaction() écrivent correctement dans
    * transaction_financiere, avant de brancher PortefeuilleRepository dans l'application. Comme
    * les précédents : hors architecture, à supprimer une fois validé.
*/
public class TesterTransactions {
    public static void main(String[] args) {
        GestionnairePostgreSQL repository = new GestionnairePostgreSQL();

        int id = repository.ajouterTransaction(15000, TypeTransaction.DEPENSE, Categorie.ALIMENTATION,
                LocalDate.now(), "Test ajout");
        System.out.println("Transaction ajoutée, id = " + id
                + " — vérifiez : une ligne dans transaction_financiere, montant 15000, categorie ALIMENTATION.");

        repository.modifierTransaction(id, 20000, Categorie.TRANSPORT, LocalDate.now(), "Test modification");
        System.out.println("Transaction " + id + " modifiée — vérifiez : montant 20000, categorie TRANSPORT, "
                + "description 'Test modification', même id.");

        repository.supprimerTransaction(id);
        System.out.println("Transaction " + id + " supprimée — vérifiez : la ligne a disparu de transaction_financiere.");

        System.out.println("Terminé.");
    }
}
