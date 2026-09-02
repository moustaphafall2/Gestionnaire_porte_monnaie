package domain.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import domain.enumeration.Categorie;

/*
    * La classe Portefeuille regroupe toutes les données de l'utilisateur : les transactions,
    * les catégories actives et les objectifs d'épargne. Elle ne porte aucun calcul, aucune règle
    * de gestion, ni la moindre recherche : uniquement sa structure, et l'ajout/retrait dans ses
    * listes et son ensemble. Tous les calculs et recherches vivent dans
    * application.service.implementation ; la persistance dans les repositories
    * d'infrastructure.persistence (CategorieRepository, TransactionRepository,
    * EpargneRepository), que chaque service appelle directement.
    *
    * ajouterTransaction()/retirerTransaction(), ajouterObjectif()/retirerObjectif() et
    * activerCategorie()/desactiverCategorie() restent ici plutôt que de disparaître au profit
    * d'un setter qui remplacerait la liste ou l'ensemble entier : un setter de collection
    * obligerait chaque service à copier la liste, la modifier, puis la réinjecter en entier pour
    * un seul ajout ou retrait — plus lourd, et pas plus sûr. Ce sont des setters d'un élément
    * d'une collection, pas des méthodes de calcul ; les getters correspondants restent des vues
    * non modifiables (Collections.unmodifiableList/unmodifiableSet) : la seule façon d'ajouter
    * ou de retirer un élément passe par ces méthodes dédiées, jamais par la liste renvoyée.
*/
public class Portefeuille {

    // Attributs
    private List<Transaction> transactions;
    private Set<Categorie> categoriesActives;
    private List<Epargne> objectifs;

    // Constructeur : crée un portefeuille vide, sans transaction ni objectif
    public Portefeuille() {
        this.transactions = new ArrayList<>();
        this.categoriesActives = new HashSet<>();
        this.objectifs = new ArrayList<>();
    }

    // Gestion des transactions

    // Vue non modifiable des transactions. Renvoie null si le champ lui-même est encore null
    // plutôt que de lever une NullPointerException en tentant de l'envelopper : aucun chemin
    // actuel ne construit un Portefeuille dans cet état (le constructeur initialise toujours les
    // trois collections), mais le vérifier ne coûte rien et protège contre un Portefeuille mal
    // construit si un futur appelant contournait un jour le constructeur.
    public List<Transaction> getTransactions() {
        return transactions == null ? null : Collections.unmodifiableList(transactions);
    }

    // Remplace l'ancien setter : ajouter une transaction est un ajout à la liste, pas une
    // substitution d'attribut.
    public void ajouterTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public void retirerTransaction(Transaction transaction) {
        transactions.remove(transaction);
    }

    // Gestion des catégories

    public Set<Categorie> getCategoriesActives() {
        return categoriesActives == null ? null : Collections.unmodifiableSet(categoriesActives);
    }

    public void activerCategorie(Categorie categorie) {
        categoriesActives.add(categorie);
    }

    // La désactivation retire seulement la catégorie des choix proposés :
    // les transactions déjà enregistrées avec cette catégorie ne sont jamais modifiées.
    public void desactiverCategorie(Categorie categorie) {
        categoriesActives.remove(categorie);
    }

    // Gestion des objectifs d'épargne

    // Remplace l'ancienne construction dans creerObjectif() : ajouter un objectif est un ajout
    // à la liste, pas une substitution d'attribut.
    public void ajouterObjectif(Epargne objectif) {
        objectifs.add(objectif);
    }

    // Retrait structurel de la liste (pas la règle de gestion "objectif vide" : celle-ci est
    // vérifiée en amont par ServiceEpargne, qui appelle cette méthode une fois la vérification
    // passée). Symétrique de retirerTransaction(Transaction).
    public void retirerObjectif(Epargne objectif) {
        objectifs.remove(objectif);
    }

    public List<Epargne> getObjectifs() {
        return objectifs == null ? null : Collections.unmodifiableList(objectifs);
    }
}
