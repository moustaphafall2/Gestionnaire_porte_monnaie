package infrastructure.persistence;

import java.time.LocalDate;

import domain.entity.Portefeuille;
import domain.enumeration.Categorie;
import domain.enumeration.SensMouvement;
import domain.enumeration.TypeTransaction;

/*
    * PortefeuilleRepository est le seul contrat que la couche application connaît de la
    * persistance : charger un portefeuille, le faire évoluer. ServicePortefeuille ne dépend que
    * de cette interface, jamais de GestionnairePostgreSQL ni de la façon dont les données sont
    * réellement stockées.
    *
    * Pas de sauvegarder(Portefeuille) qui réécrirait l'intégralité des données à chaque
    * opération : une méthode granulaire par mutation, pour catégories, transactions et épargne.
*/
public interface PortefeuilleRepository {
    public Portefeuille charger();

    public void activerCategorie(Categorie categorie);
    public void desactiverCategorie(Categorie categorie);

    public int ajouterTransaction(double montant, TypeTransaction type, Categorie categorie, LocalDate date, String description);
    public void modifierTransaction(int id, double montant, Categorie categorie, LocalDate date, String description);
    public void supprimerTransaction(int id);

    public int ajouterObjectif(String nom, double montantCible, LocalDate dateLimite);
    public void ajouterMouvement(int idObjectif, double montant, SensMouvement sens, LocalDate date);
    public void supprimerObjectif(int id);
}
