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
    * Il porte aussi, depuis cette étape, toute la validation qui vivait auparavant dans
    * Transaction (montant positif, date pas dans le futur, catégorie cohérente avec le type,
    * description jamais nulle) : Transaction ne se protège plus elle-même, c'est ce service qui
    * garantit qu'aucune Transaction invalide ne peut être construite ou modifiée.
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

    // Anciennement Transaction.validerId(). L'identifiant est toujours généré par
    // Portefeuille.genererIdTransaction(), donc toujours strictement positif en pratique ; ce
    // contrôle reste défensif, au cas où cette génération changerait un jour.
    private void validerId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'identifiant doit être strictement positif.");
        }
    }

    // Anciennement Transaction.validerMontant(). Appelée à la fois pour un ajout et pour une
    // modification.
    private void validerMontant(double montant) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif.");
        }
    }

    // Anciennement Transaction.validerType(). Le type n'est jamais fourni par l'utilisateur
    // (toujours TypeTransaction.DEPENSE ou REVENU, choisi par ajouterDepense()/ajouterRevenu()),
    // et il ne change jamais après création (pas de setType()) : ce contrôle ne sert donc qu'à
    // l'ajout, jamais à la modification.
    private void validerType(TypeTransaction type) {
        if (type == null) {
            throw new IllegalArgumentException("Le type de transaction est obligatoire.");
        }
    }

    // Anciennement Transaction.validerCategorie(). Règle de gestion "catégorie cohérente" :
    // appelée à la fois pour un ajout (contre le type choisi) et pour une modification (contre
    // le type de la transaction existante, qui ne change jamais).
    private void validerCategorieCoherente(Categorie categorie, TypeTransaction type) {
        if (categorie == null) {
            throw new IllegalArgumentException("La catégorie est obligatoire.");
        }
        if (categorie.getType() != type) {
            throw new IllegalArgumentException("La catégorie ne correspond pas au type de la transaction.");
        }
    }

    // Anciennement Transaction.validerDate(). Appelée à la fois pour un ajout et pour une
    // modification.
    private void validerDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("La date est obligatoire.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date ne peut pas être dans le futur.");
        }
    }

    // Une description absente devient une chaîne vide plutôt que null : Transaction ne porte
    // plus aucune règle sur ses propres champs, y compris celle-ci.
    private String normaliserDescription(String description) {
        return description == null ? "" : description;
    }

    public Transaction ajouterDepense(double montant, Categorie categorie, LocalDate date, String description) {
        validerCategorieActive(categorie);
        validerMontant(montant);
        validerType(TypeTransaction.DEPENSE);
        validerCategorieCoherente(categorie, TypeTransaction.DEPENSE);
        validerDate(date);

        Portefeuille portefeuille = servicePortefeuille.getDonnees();
        int id = portefeuille.genererIdTransaction();
        validerId(id);
        Transaction depense = new Transaction(id, montant, TypeTransaction.DEPENSE, categorie, date, normaliserDescription(description));
        portefeuille.ajouterTransaction(depense);
        servicePortefeuille.sauvegarder();
        return depense;
    }

    public Transaction ajouterRevenu(double montant, Categorie categorie, LocalDate date, String description) {
        validerCategorieActive(categorie);
        validerMontant(montant);
        validerType(TypeTransaction.REVENU);
        validerCategorieCoherente(categorie, TypeTransaction.REVENU);
        validerDate(date);

        Portefeuille portefeuille = servicePortefeuille.getDonnees();
        int id = portefeuille.genererIdTransaction();
        validerId(id);
        Transaction revenu = new Transaction(id, montant, TypeTransaction.REVENU, categorie, date, normaliserDescription(description));
        portefeuille.ajouterTransaction(revenu);
        servicePortefeuille.sauvegarder();
        return revenu;
    }

    public void modifierTransaction(int id, double nouveauMontant, Categorie nouvelleCategorie, LocalDate nouvelleDate, String nouvelleDescription) {
        validerCategorieActive(nouvelleCategorie);
        Transaction transaction = trouverTransaction(id);
        validerMontant(nouveauMontant);
        validerCategorieCoherente(nouvelleCategorie, transaction.getType());
        validerDate(nouvelleDate);

        transaction.setMontant(nouveauMontant);
        transaction.setCategorie(nouvelleCategorie);
        transaction.setDate(nouvelleDate);
        transaction.setDescription(normaliserDescription(nouvelleDescription));
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
