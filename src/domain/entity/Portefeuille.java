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
    * de gestion, ni la moindre recherche : uniquement sa structure, l'ajout/retrait dans ses
    * listes et son ensemble, et l'accès à ses compteurs d'identifiants. Tous les calculs et
    * recherches vivent dans application.service.implementation ; la persistance dans
    * ServicePortefeuille, seul à détenir le PortefeuilleRepository et à déclencher la
    * sauvegarde ; la réparation après désérialisation Gson dans GestionnaireFichier — un
    * contournement d'un comportement de Gson, propre à la persistance, qui n'a rien à voir avec
    * une règle du domaine.
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
    private int prochainIdTransaction;
    private int prochainIdObjectif;

    // Constructeur : crée un portefeuille vide, sans transaction ni objectif
    public Portefeuille() {
        this.transactions = new ArrayList<>();
        this.categoriesActives = new HashSet<>();
        this.objectifs = new ArrayList<>();
        this.prochainIdTransaction = 1;
        this.prochainIdObjectif = 1;
    }

    // Gestion des transactions

    // Vue non modifiable des transactions. Renvoie null si le champ lui-même est encore null
    // (juste après une désérialisation Gson, avant réparation par GestionnaireFichier) plutôt
    // que de lever une NullPointerException en tentant de l'envelopper.
    public List<Transaction> getTransactions() {
        return transactions == null ? null : Collections.unmodifiableList(transactions);
    }

    // Setter classique du compteur : la génération de l'identifiant suivant (lire puis
    // incrémenter) est désormais un traitement porté par ServiceTransaction, pas par l'entité.
    public int getProchainIdTransaction() {
        return prochainIdTransaction;
    }

    public void setProchainIdTransaction(int prochainIdTransaction) {
        this.prochainIdTransaction = prochainIdTransaction;
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

    // Setter classique du compteur, même principe que getProchainIdTransaction() ci-dessus.
    public int getProchainIdObjectif() {
        return prochainIdObjectif;
    }

    public void setProchainIdObjectif(int prochainIdObjectif) {
        this.prochainIdObjectif = prochainIdObjectif;
    }

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

    // Réparation après désérialisation Gson : voir GestionnaireFichier, seule appelante. Ces
    // trois setters remplacent le champ entier plutôt que d'ajouter un élément — volontairement
    // différents de ajouterTransaction()/ajouterObjectif()/activerCategorie() ci-dessus, qui
    // restent la seule façon normale de faire évoluer ces collections une fois le portefeuille
    // chargé. N'importe quel autre appelant qui s'en servirait remplacerait toute la liste ou
    // l'ensemble d'un coup, en perdant son contenu existant.
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void setCategoriesActives(Set<Categorie> categoriesActives) {
        this.categoriesActives = categoriesActives;
    }

    public void setObjectifs(List<Epargne> objectifs) {
        this.objectifs = objectifs;
    }
}
