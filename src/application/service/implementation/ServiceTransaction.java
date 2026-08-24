package application.service.implementation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import domain.entity.Portefeuille;
import domain.entity.Transaction;
import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
import application.service.interfaces.IServiceTransaction;

/*
    * ServiceTransaction porte les règles de gestion des dépenses et des revenus : ajout,
    * modification, suppression, consultation et filtrage. Déplacé depuis Portefeuille.
    *
    * Il ne détient jamais Portefeuille en attribut : il passe par
    * servicePortefeuille.getDonnees() à chaque appel, comme le veut la règle du projet.
*/
public class ServiceTransaction implements IServiceTransaction {
    private final ServicePortefeuille servicePortefeuille;
    private final ServiceCategorie serviceCategorie;

    public ServiceTransaction(ServicePortefeuille servicePortefeuille, ServiceCategorie serviceCategorie) {
        this.servicePortefeuille = servicePortefeuille;
        this.serviceCategorie = serviceCategorie;
    }

    // Une transaction ne peut porter qu'une catégorie active : une catégorie désactivée
    // entre-temps ne doit plus pouvoir être choisie pour une nouvelle transaction (mais les
    // transactions déjà enregistrées avec elle restent inchangées, cf. desactiverCategorie).
    private void validerCategorieActive(Categorie categorie) {
        if (!serviceCategorie.estActive(categorie)) {
            throw new IllegalStateException("La catégorie \"" + categorie.getLibelle() + "\" n'est pas active.");
        }
    }

    public Transaction ajouterDepense(double montant, Categorie categorie, LocalDate date, String description) {
        validerCategorieActive(categorie);
        Portefeuille portefeuille = servicePortefeuille.getDonnees();
        Transaction depense = new Transaction(portefeuille.genererIdTransaction(), montant, TypeTransaction.DEPENSE, categorie, date, description);
        portefeuille.ajouterTransaction(depense);
        servicePortefeuille.sauvegarder();
        return depense;
    }

    public Transaction ajouterRevenu(double montant, Categorie categorie, LocalDate date, String description) {
        validerCategorieActive(categorie);
        Portefeuille portefeuille = servicePortefeuille.getDonnees();
        Transaction revenu = new Transaction(portefeuille.genererIdTransaction(), montant, TypeTransaction.REVENU, categorie, date, description);
        portefeuille.ajouterTransaction(revenu);
        servicePortefeuille.sauvegarder();
        return revenu;
    }

    public void modifierTransaction(int id, double nouveauMontant, Categorie nouvelleCategorie, LocalDate nouvelleDate, String nouvelleDescription) {
        validerCategorieActive(nouvelleCategorie);
        Transaction transaction = trouverTransaction(id);
        transaction.setMontant(nouveauMontant);
        transaction.setCategorie(nouvelleCategorie);
        transaction.setDate(nouvelleDate);
        transaction.setDescription(nouvelleDescription);
        servicePortefeuille.sauvegarder();
    }

    public void supprimerTransaction(int id) {
        Transaction transaction = trouverTransaction(id);
        servicePortefeuille.getDonnees().retirerTransaction(transaction);
        servicePortefeuille.sauvegarder();
    }

    // Recherche publique d'une transaction par id, utilisée par ControleurTransaction pour
    // connaître le type de la transaction avant de proposer les catégories actives compatibles,
    // lors d'une modification.
    public Transaction getTransaction(int id) {
        return trouverTransaction(id);
    }

    // Historique complet, trié du plus récent au plus ancien
    public List<Transaction> getHistorique() {
        List<Transaction> historique = new ArrayList<>(servicePortefeuille.getDonnees().getTransactions());
        historique.sort(Comparator.comparing(Transaction::getDate).reversed());
        return historique;
    }

    public List<Transaction> filtrerParDate(LocalDate debut, LocalDate fin) {
        List<Transaction> resultat = new ArrayList<>();
        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            LocalDate date = transaction.getDate();
            if (!date.isBefore(debut) && !date.isAfter(fin)) {
                resultat.add(transaction);
            }
        }
        return resultat;
    }

    public List<Transaction> filtrerParCategorie(Categorie categorie) {
        List<Transaction> resultat = new ArrayList<>();
        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            if (transaction.getCategorie() == categorie) {
                resultat.add(transaction);
            }
        }
        return resultat;
    }

    public List<Transaction> filtrerParType(TypeTransaction type) {
        List<Transaction> resultat = new ArrayList<>();
        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            if (transaction.getType() == type) {
                resultat.add(transaction);
            }
        }
        return resultat;
    }

    // Recherche interne d'une transaction par id, réutilisée par modifierTransaction et supprimerTransaction
    private Transaction trouverTransaction(int id) {
        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            if (transaction.getId() == id) {
                return transaction;
            }
        }
        throw new IllegalArgumentException("Aucune transaction avec l'identifiant " + id + ".");
    }
}
