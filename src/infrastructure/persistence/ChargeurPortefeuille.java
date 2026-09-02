package infrastructure.persistence;

import domain.entity.Epargne;
import domain.entity.Portefeuille;
import domain.entity.Transaction;
import domain.enumeration.Categorie;

/*
    * ChargeurPortefeuille reconstruit le Portefeuille en mémoire au démarrage de l'application,
    * à partir des trois repositories. Fusion de l'ancienne interface PortefeuilleRepository et
    * de son implémentation GestionnairePostgreSQL : avec un repository par entité et aucune
    * interface pour les repositories (décision de la maîtresse de stage), garder une interface
    * pour cette seule classe aurait été incohérent.
    *
    * Ne contient aucun SQL : chaque repository sait déjà charger sa propre table et traduire ses
    * erreurs en ErreurChargementException, cette classe n'a plus qu'à assembler les trois
    * résultats dans un Portefeuille neuf.
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

    // Reconstruit le portefeuille en une seule fois au démarrage. Aucune "réparation après
    // chargement" n'est nécessaire : Portefeuille() initialise déjà ses listes vides dans son
    // constructeur, et chaque élément lu passe par une méthode d'ajout de l'entité
    // (activerCategorie, ajouterTransaction, ajouterObjectif) — jamais par un contournement du
    // constructeur.
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
