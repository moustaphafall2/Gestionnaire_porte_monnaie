package infrastructure.persistence;

import domain.entity.Epargne;
import domain.entity.Portefeuille;
import domain.entity.Transaction;
import domain.enumeration.Categorie;

/*
    * ChargeurPortefeuille reconstruit le Portefeuille en mémoire au démarrage, à partir des
    * trois repositories. Ne contient aucun SQL.
*/
public class ChargeurPortefeuille {
    private final CategorieRepository categorieRepository;
    private final TransactionRepository transactionRepository;
    private final EpargneRepository epargneRepository;

    public ChargeurPortefeuille(CategorieRepository categorieRepository, TransactionRepository transactionRepository,
            EpargneRepository epargneRepository) {
        this.categorieRepository = categorieRepository;
        this.transactionRepository = transactionRepository;
        this.epargneRepository = epargneRepository;
    }

    public Portefeuille charger() {
        Portefeuille portefeuille = new Portefeuille();

        for (Categorie categorie : categorieRepository.chargerActives()) {
            portefeuille.activerCategorie(categorie);
        }
        for (Transaction transaction : transactionRepository.chargerToutes()) {
            portefeuille.ajouterTransaction(transaction);
        }
        for (Epargne objectif : epargneRepository.chargerTous()) {
            portefeuille.ajouterObjectif(objectif);
        }

        return portefeuille;
    }
}
