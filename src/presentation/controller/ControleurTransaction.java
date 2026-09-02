package presentation.controller;

import java.time.LocalDate;

import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
import application.service.interfaces.IServiceCategorie;
import application.service.interfaces.IServiceSolde;
import application.service.interfaces.IServiceTransaction;
import presentation.view.VueTransaction;

/*
    * ControleurTransaction porte les huit actions de l'écran transactions : ajouter une dépense,
    * ajouter un revenu, afficher l'historique complet, le filtrer par date/catégorie/type,
    * modifier une transaction, supprimer une transaction.
*/
public class ControleurTransaction {
    private final VueTransaction vueTransaction;
    private final IServiceTransaction serviceTransaction;
    private final IServiceCategorie serviceCategorie;
    private final IServiceSolde serviceSolde;

    public ControleurTransaction(VueTransaction vueTransaction, IServiceTransaction serviceTransaction,
            IServiceCategorie serviceCategorie, IServiceSolde serviceSolde) {
        this.vueTransaction = vueTransaction;
        this.serviceTransaction = serviceTransaction;
        this.serviceCategorie = serviceCategorie;
        this.serviceSolde = serviceSolde;
    }

    public void ajouterDepense() {
        if (!serviceCategorie.aCategorieActiveDeType(TypeTransaction.DEPENSE)) {
            vueTransaction.afficherAucuneCategorieActive(TypeTransaction.DEPENSE);
            return;
        }

        Categorie categorie = vueTransaction.demanderCategorie(serviceCategorie.getCategoriesActivesDeType(TypeTransaction.DEPENSE));
        double montant = vueTransaction.demanderMontantDepense();
        LocalDate date = vueTransaction.demanderDate();
        String description = vueTransaction.demanderDescription();

        vueTransaction.afficherRecapitulatif(montant, categorie, date);

        // Règle de gestion : autorisée malgré l'avertissement, car une dépense constate un fait déjà survenu, pas une décision prise à l'instant.
        if (serviceSolde.depenseRendraSoldeNegatif(montant)) {
            vueTransaction.afficherAvertissementSoldeNegatif(serviceSolde.soldeApresDepense(montant));
        }

        if (!vueTransaction.demanderConfirmationDepense()) {
            vueTransaction.afficherOperationAnnulee();
            return;
        }

        serviceTransaction.ajouterDepense(montant, categorie, date, description);
        vueTransaction.afficherDepenseEnregistree();
    }

    public void ajouterRevenu() {
        if (!serviceCategorie.aCategorieActiveDeType(TypeTransaction.REVENU)) {
            vueTransaction.afficherAucuneCategorieActive(TypeTransaction.REVENU);
            return;
        }

        Categorie categorie = vueTransaction.demanderCategorie(serviceCategorie.getCategoriesActivesDeType(TypeTransaction.REVENU));
        double montant = vueTransaction.demanderMontantRevenu();
        LocalDate date = vueTransaction.demanderDate();
        String description = vueTransaction.demanderDescription();

        vueTransaction.afficherRecapitulatif(montant, categorie, date);

        if (!vueTransaction.demanderConfirmationRevenu()) {
            vueTransaction.afficherOperationAnnulee();
            return;
        }

        serviceTransaction.ajouterRevenu(montant, categorie, date, description);
        vueTransaction.afficherRevenuEnregistre();
    }

    public void afficherHistoriqueComplet() {
        vueTransaction.afficherTransactions(serviceTransaction.getHistorique());
    }

    public void afficherHistoriqueParDate() {
        LocalDate debut = vueTransaction.demanderDateDebut();
        LocalDate fin = vueTransaction.demanderDateFin();
        vueTransaction.afficherTransactions(serviceTransaction.filtrerParDate(debut, fin));
    }

    public void afficherHistoriqueParCategorie() {
        vueTransaction.afficherTransactions(serviceTransaction.filtrerParCategorie(vueTransaction.demanderCategorieParmiToutes()));
    }

    public void afficherHistoriqueParType() {
        vueTransaction.afficherTransactions(serviceTransaction.filtrerParType(vueTransaction.demanderType()));
    }

    public void modifierTransaction() {
        afficherHistoriqueComplet();
        int id = vueTransaction.demanderIdentifiantAModifier();
        if (id == 0) {
            return;
        }

        try {
            TypeTransaction type = serviceTransaction.getTransaction(id).getType();
            if (!serviceCategorie.aCategorieActiveDeType(type)) {
                vueTransaction.afficherAucuneCategorieActive(type);
                return;
            }

            double montant = vueTransaction.demanderNouveauMontant();
            LocalDate date = vueTransaction.demanderNouvelleDate();
            Categorie categorie = vueTransaction.demanderCategorie(serviceCategorie.getCategoriesActivesDeType(type));
            String description = vueTransaction.demanderNouvelleDescription();
            serviceTransaction.modifierTransaction(id, montant, categorie, date, description);
            vueTransaction.afficherTransactionModifiee();
        } catch (IllegalArgumentException | IllegalStateException erreur) {
            vueTransaction.afficherErreur(erreur.getMessage());
        }
    }

    public void supprimerTransaction() {
        afficherHistoriqueComplet();
        int id = vueTransaction.demanderIdentifiantASupprimer();
        if (id == 0) {
            return;
        }

        try {
            if (vueTransaction.demanderConfirmationSuppression()) {
                serviceTransaction.supprimerTransaction(id);
                vueTransaction.afficherTransactionSupprimee();
            } else {
                vueTransaction.afficherOperationAnnulee();
            }
        } catch (IllegalArgumentException | IllegalStateException erreur) {
            vueTransaction.afficherErreur(erreur.getMessage());
        }
    }
}
