package application.service.implementation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import application.dto.TransactionDTO;
import application.mapper.TransactionMapper;
import domain.entity.Transaction;
import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
import application.service.interfaces.IServiceTransaction;
import infrastructure.persistence.TransactionRepository;

/*
    * ServiceTransaction porte les règles de gestion des dépenses et des revenus : ajout,
    * modification, suppression, consultation et filtrage. Ne renvoie jamais Transaction à la
    * présentation, seulement des TransactionDTO construits par TransactionMapper.
*/
public class ServiceTransaction implements IServiceTransaction {
    private final ServicePortefeuille servicePortefeuille;
    private final ServiceCategorie serviceCategorie;
    private final TransactionRepository transactionRepository;

    public ServiceTransaction(ServicePortefeuille servicePortefeuille, ServiceCategorie serviceCategorie,
            TransactionRepository transactionRepository) {
        this.servicePortefeuille = servicePortefeuille;
        this.serviceCategorie = serviceCategorie;
        this.transactionRepository = transactionRepository;
    }

    // Règle de gestion : une transaction ne peut porter qu'une catégorie active.
    private void validerCategorieActive(Categorie categorie) {
        if (!serviceCategorie.estActive(categorie)) {
            throw new IllegalStateException("La catégorie \"" + categorie.getLibelle() + "\" n'est pas active.");
        }
    }

    private void validerId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("L'identifiant doit être strictement positif.");
        }
    }

    private void validerMontant(double montant) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif.");
        }
    }

    private void validerType(TypeTransaction type) {
        if (type == null) {
            throw new IllegalArgumentException("Le type de transaction est obligatoire.");
        }
    }

    // Règle de gestion "catégorie cohérente" : la catégorie doit être du même type que la
    // transaction.
    private void validerCategorieCoherente(Categorie categorie, TypeTransaction type) {
        if (categorie == null) {
            throw new IllegalArgumentException("La catégorie est obligatoire.");
        }
        if (categorie.getType() != type) {
            throw new IllegalArgumentException("La catégorie ne correspond pas au type de la transaction.");
        }
    }

    private void validerDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("La date est obligatoire.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date ne peut pas être dans le futur.");
        }
    }

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
        int id = transactionRepository.ajouter(montant, TypeTransaction.DEPENSE, categorie, date, descriptionNormalisee);
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
        int id = transactionRepository.ajouter(montant, TypeTransaction.REVENU, categorie, date, descriptionNormalisee);
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
        transactionRepository.modifier(id, nouveauMontant, nouvelleCategorie, nouvelleDate, descriptionNormalisee);

        transaction.setMontant(nouveauMontant);
        transaction.setCategorie(nouvelleCategorie);
        transaction.setDate(nouvelleDate);
        transaction.setDescription(descriptionNormalisee);
    }

    public void supprimerTransaction(int id) {
        Transaction transaction = trouverTransaction(id);
        transactionRepository.supprimer(id);
        servicePortefeuille.getDonnees().retirerTransaction(transaction);
    }

    public TransactionDTO getTransaction(int id) {
        return TransactionMapper.versDTO(trouverTransaction(id));
    }

    public List<TransactionDTO> getHistorique() {
        List<Transaction> historique = new ArrayList<>(servicePortefeuille.getDonnees().getTransactions());
        historique.sort(Comparator.comparing(Transaction::getDate).reversed());
        return TransactionMapper.versListeDTO(historique);
    }

    public List<TransactionDTO> filtrerParDate(LocalDate debut, LocalDate fin) {
        List<Transaction> resultat = new ArrayList<>();
        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            LocalDate date = transaction.getDate();
            if (!date.isBefore(debut) && !date.isAfter(fin)) {
                resultat.add(transaction);
            }
        }
        return TransactionMapper.versListeDTO(resultat);
    }

    public List<TransactionDTO> filtrerParCategorie(Categorie categorie) {
        List<Transaction> resultat = new ArrayList<>();
        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            if (transaction.getCategorie() == categorie) {
                resultat.add(transaction);
            }
        }
        return TransactionMapper.versListeDTO(resultat);
    }

    public List<TransactionDTO> filtrerParType(TypeTransaction type) {
        List<Transaction> resultat = new ArrayList<>();
        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            if (transaction.getType() == type) {
                resultat.add(transaction);
            }
        }
        return TransactionMapper.versListeDTO(resultat);
    }

    private Transaction trouverTransaction(int id) {
        for (Transaction transaction : servicePortefeuille.getDonnees().getTransactions()) {
            if (transaction.getId() == id) {
                return transaction;
            }
        }
        throw new IllegalArgumentException("Aucune transaction avec l'identifiant " + id + ".");
    }
}
