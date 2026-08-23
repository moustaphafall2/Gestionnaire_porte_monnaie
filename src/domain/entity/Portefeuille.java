package domain.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import domain.enumeration.Categorie;

/*
    * La classe Portefeuille regroupe toutes les données de l'utilisateur : les transactions,
    * les catégories actives et les objectifs d'épargne. Elle ne porte plus aucun calcul ni
    * règle de gestion : uniquement sa structure, l'ajout/retrait dans ses listes et son
    * ensemble, la génération des identifiants, et l'accès par clé (getObjectif). Tous les
    * calculs vivent dans application.service.implementation, et la persistance dans ServicePortefeuille, seul à
    * détenir le PortefeuilleRepository et à déclencher la sauvegarde.
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

    // Vue brute des transactions, utilisée par les services pour faire leurs calculs.
    // L'entité ne fait ici que donner accès à ses données.
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    // Distribue l'identifiant suivant et avance le compteur. Le compteur reste ici (champ de
    // l'entité, donc sauvegardé) : s'il repartait de zéro au redémarrage, on créerait des
    // doublons. Mais la construction de la Transaction elle-même se fait dans ServiceTransaction.
    public int genererIdTransaction() {
        return prochainIdTransaction++;
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
        return Collections.unmodifiableSet(categoriesActives);
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

    // Distribue l'identifiant suivant et avance le compteur, comme genererIdTransaction().
    // La construction de l'Epargne elle-même se fait dans ServiceEpargne.
    public int genererIdObjectif() {
        return prochainIdObjectif++;
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
        return Collections.unmodifiableList(objectifs);
    }

    // Recherche publique d'un objectif par id, utilisée par ServiceEpargne pour retrouver
    // l'objectif choisi avant de contribuer, retirer, supprimer ou en afficher le détail.
    public Epargne getObjectif(int idObjectif) {
        return trouverObjectif(idObjectif);
    }

    // Recherche interne d'un objectif par id, utilisée par getObjectif()
    private Epargne trouverObjectif(int idObjectif) {
        for (Epargne objectif : objectifs) {
            if (objectif.getId() == idObjectif) {
                return objectif;
            }
        }
        throw new IllegalArgumentException("Aucun objectif avec l'identifiant " + idObjectif + ".");
    }

    // Gson contourne le constructeur à la désérialisation (il remplit les champs directement) :
    // un champ absent du JSON, ou explicitement "null", reste à null au lieu d'être initialisé
    // à une collection vide. Utilisée uniquement par GestionnaireFichier.charger(), juste après
    // la désérialisation, pour garantir qu'un Portefeuille rechargé est toujours exploitable
    // (getTransactions()/getCategoriesActives()/getObjectifs() ne doivent jamais lever de
    // NullPointerException).
    public void reparerApresChargement() {
        if (transactions == null) {
            transactions = new ArrayList<>();
        }
        if (categoriesActives == null) {
            categoriesActives = new HashSet<>();
        }
        if (objectifs == null) {
            objectifs = new ArrayList<>();
        }
    }
}
