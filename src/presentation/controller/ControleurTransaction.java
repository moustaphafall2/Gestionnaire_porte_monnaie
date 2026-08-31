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
    * modifier une transaction, supprimer une transaction. Chaque méthode publique se lit de haut
    * en bas sans dépendre d'une autre méthode privée ; modifierTransaction() et
    * supprimerTransaction() réutilisent afficherHistoriqueComplet() (méthode publique sœur, pas
    * une méthode privée partagée) pour montrer la liste avant de demander un identifiant.
    *
    * La reprise après un échec de sauvegarde n'est plus gérée ici : ErreurSauvegardeException
    * n'est attrapée nulle part dans cette classe, elle remonte jusqu'à Main, qui la traite une
    * seule fois pour toutes les actions du programme. Les exceptions métier
    * (IllegalArgumentException, IllegalStateException), elles, restent attrapées ici : ce sont
    * des erreurs propres à l'action en cours, pas un problème de disque.
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

        // Règle de gestion "dépense > solde ⇒ avertissement" : le seuil est calculé par
        // ServiceSolde.depenseRendraSoldeNegatif(), pas ici. Le contrôleur ne fait que brancher
        // sur ce booléen, exactement comme pour ServiceEpargne.depasseraCible().
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
            // La catégorie proposée doit être active (règle de gestion "catégorie cohérente") :
            // une catégorie inactive choisie ici serait rejetée par IServiceTransaction, après
            // coup, une fois toutes les saisies déjà faites. On restreint donc aux catégories
            // actives du même type que la transaction existante.
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
