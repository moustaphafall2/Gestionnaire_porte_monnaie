package domain.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import domain.enumeration.Categorie;

/*
    * Portefeuille regroupe les données de l'utilisateur : transactions, catégories actives,
    * objectifs d'épargne. Aucun calcul, aucune règle de gestion : uniquement la structure et
    * l'ajout/retrait dans ses collections, exposées en vues non modifiables.
*/
public class Portefeuille {

    private List<Transaction> transactions;
    private Set<Categorie> categoriesActives;
    private List<Epargne> objectifs;

    public Portefeuille() {
        this.transactions = new ArrayList<>();
        this.categoriesActives = new HashSet<>();
        this.objectifs = new ArrayList<>();
    }

    // Vérification défensive : le constructeur initialise toujours ce champ, mais la vérifier
    // ne coûte rien et protège contre un Portefeuille mal construit.
    public List<Transaction> getTransactions() {
        return transactions == null ? null : Collections.unmodifiableList(transactions);
    }

    public void ajouterTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public void retirerTransaction(Transaction transaction) {
        transactions.remove(transaction);
    }

    public Set<Categorie> getCategoriesActives() {
        return categoriesActives == null ? null : Collections.unmodifiableSet(categoriesActives);
    }

    public void activerCategorie(Categorie categorie) {
        categoriesActives.add(categorie);
    }

    // Règle de gestion : la désactivation n'a aucun effet sur les transactions déjà enregistrées.
    public void desactiverCategorie(Categorie categorie) {
        categoriesActives.remove(categorie);
    }

    public void ajouterObjectif(Epargne objectif) {
        objectifs.add(objectif);
    }

    public void retirerObjectif(Epargne objectif) {
        objectifs.remove(objectif);
    }

    public List<Epargne> getObjectifs() {
        return objectifs == null ? null : Collections.unmodifiableList(objectifs);
    }
}
