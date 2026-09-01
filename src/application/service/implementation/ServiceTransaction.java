package application.service.implementation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import application.dto.TransactionDTO;
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
    *
    * Depuis l'étape DTO, il ne renvoie plus jamais de Transaction à la présentation : chaque
    * méthode consultée par ControleurTransaction renvoie un TransactionDTO, construit par
    * versAffichage() juste avant de sortir du service. La vue ne reçoit donc jamais l'entité.
    *
    * Depuis l'étape 6, l'identifiant d'une nouvelle transaction vient de la base
    * (servicePortefeuille.enregistrerNouvelleTransaction(), qui appelle PortefeuilleRepository) :
    * il n'existe plus de compteur à lire avant de construire l'objet. Conséquence sur l'ordre des
    * opérations, systématique désormais : persister d'abord, construire ou muter la mémoire
    * ensuite — jamais l'inverse, contrairement à avant cette étape.
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

    // Anciennement Transaction.validerId(). L'identifiant est toujours généré par la base
    // (servicePortefeuille.enregistrerNouvelleTransaction()), donc toujours strictement positif
    // en pratique ; ce contrôle reste défensif, au cas où cette génération changerait un jour.
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

    public void ajouterDepense(double montant, Categorie categorie, LocalDate date, String description) {
        validerCategorieActive(categorie);
        validerMontant(montant);
        validerType(TypeTransaction.DEPENSE);
        validerCategorieCoherente(categorie, TypeTransaction.DEPENSE);
        validerDate(date);

        String descriptionNormalisee = normaliserDescription(description);
        int id = servicePortefeuille.enregistrerNouvelleTransaction(montant, TypeTransaction.DEPENSE, categorie, date, descriptionNormalisee);
        validerId(id);
        Transaction depense = new Transaction(id, montant, TypeTransaction.DEPENSE, categorie, date, descriptionNormalisee);
        servicePortefeuille.getDonnees().ajouterTransaction(depense);
    }

    public void ajouterRevenu(double montant, Categorie categorie, LocalDate date, String description) {
        validerCategorieActive(categorie);
        validerMontant(montant);
        validerType(TypeTransaction.REVENU);
        validerCategorieCoherente(categorie, TypeTransaction.REVENU);
        validerDate(date);

        String descriptionNormalisee = normaliserDescription(description);
        int id = servicePortefeuille.enregistrerNouvelleTransaction(montant, TypeTransaction.REVENU, categorie, date, descriptionNormalisee);
        validerId(id);
        Transaction revenu = new Transaction(id, montant, TypeTransaction.REVENU, categorie, date, descriptionNormalisee);
        servicePortefeuille.getDonnees().ajouterTransaction(revenu);
    }

    public void modifierTransaction(int id, double nouveauMontant, Categorie nouvelleCategorie, LocalDate nouvelleDate, String nouvelleDescription) {
        validerCategorieActive(nouvelleCategorie);
        Transaction transaction = trouverTransaction(id);
        validerMontant(nouveauMontant);
        validerCategorieCoherente(nouvelleCategorie, transaction.getType());
        validerDate(nouvelleDate);

        String descriptionNormalisee = normaliserDescription(nouvelleDescription);
        servicePortefeuille.enregistrerModificationTransaction(id, nouveauMontant, nouvelleCategorie, nouvelleDate, descriptionNormalisee);

        transaction.setMontant(nouveauMontant);
        transaction.setCategorie(nouvelleCategorie);
        transaction.setDate(nouvelleDate);
        transaction.setDescription(descriptionNormalisee);
    }

    public void supprimerTransaction(int id) {
        Transaction transaction = trouverTransaction(id);
        servicePortefeuille.enregistrerSuppressionTransaction(id);
        servicePortefeuille.getDonnees().retirerTransaction(transaction);
    }

    // Recherche publique d'une transaction par id, utilisée par ControleurTransaction pour
    // connaître le type de la transaction avant de proposer les catégories actives compatibles,
    // lors d'une modification. Renvoie le DTO, jamais l'entité elle-même.
    public TransactionDTO getTransaction(int id) {
        return versAffichage(trouverTransaction(id));
    }

    // Historique complet, trié du plus récent au plus ancien
    public List<TransactionDTO> getHistorique() {
        List<Transaction> historique = new ArrayList<>(servicePortefeuille.getDonnees().getTransactions());
        historique.sort(Comparator.comparing(Transaction::getDate).reversed());

        List<TransactionDTO> resultat = new ArrayList<>();
        for (Transaction transaction : historique) {
            resultat.add(versAffichage(transaction));
        }
        return resultat;
    }

    public List<TransactionDTO> filtrerParDate(LocalDate debut, LocalDate fin) {
        List<TransactionDTO> resultat = new ArrayList<>();
        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            LocalDate date = transaction.getDate();
            if (!date.isBefore(debut) && !date.isAfter(fin)) {
                resultat.add(versAffichage(transaction));
            }
        }
        return resultat;
    }

    public List<TransactionDTO> filtrerParCategorie(Categorie categorie) {
        List<TransactionDTO> resultat = new ArrayList<>();
        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            if (transaction.getCategorie() == categorie) {
                resultat.add(versAffichage(transaction));
            }
        }
        return resultat;
    }

    public List<TransactionDTO> filtrerParType(TypeTransaction type) {
        List<TransactionDTO> resultat = new ArrayList<>();
        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            if (transaction.getType() == type) {
                resultat.add(versAffichage(transaction));
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

    // Construit le DTO transmis à la présentation à partir d'une Transaction du domaine : une
    // simple copie des champs, sans calcul ni mise en forme (le formatage reste du ressort de
    // VueTransaction).
    private TransactionDTO versAffichage(Transaction transaction) {
        return new TransactionDTO(transaction.getId(), transaction.getMontant(), transaction.getType(),
                transaction.getCategorie(), transaction.getDate(), transaction.getDescription());
    }
}
