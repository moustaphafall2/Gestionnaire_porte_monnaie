package presentation.controller;

import java.time.LocalDate;

import domain.enumeration.Categorie;
import domain.enumeration.TypeTransaction;
import exception.ErreurSauvegardeException;
import application.service.interfaces.IServiceCategorie;
import application.service.interfaces.IServicePortefeuille;
import application.service.interfaces.IServiceTransaction;
import presentation.view.VueTransaction;

/*
    * ControleurTransaction enchaîne les écrans "Ajouter une dépense", "Ajouter un revenu" et
    * "Voir l'historique des transactions" (consultation, filtres, modification, suppression) :
    * il lit les saisies via VueTransaction, applique les règles en appelant IServiceCategorie
    * (catégorie active), IServicePortefeuille (solde) et IServiceTransaction (enregistrement,
    * filtrage, modification, suppression), et transmet le résultat à la vue. Il n'affiche
    * jamais rien lui-même et ne contient aucun calcul métier.
    *
    * Ces écrans modifient réellement les données du portefeuille (sauf la simple consultation
    * de l'historique). Si la sauvegarde échoue après une opération déjà appliquée en mémoire
    * (voir ErreurSauvegardeException), le contrôleur propose à l'utilisateur de réessayer
    * l'écriture sur le disque, tant qu'il l'accepte, sans jamais rejouer l'opération elle-même
    * (ce qui créerait un doublon ou une double suppression). S'il refuse, l'application
    * continue normalement : ce n'est pas bloquant.
*/
public class ControleurTransaction extends ControleurConsole {
    private VueTransaction vueTransaction;
    private IServiceTransaction serviceTransaction;
    private IServiceCategorie serviceCategorie;

    public ControleurTransaction(VueTransaction vueTransaction, IServiceTransaction serviceTransaction,
            IServiceCategorie serviceCategorie, IServicePortefeuille servicePortefeuille) {
        super(vueTransaction, servicePortefeuille);
        this.vueTransaction = vueTransaction;
        this.serviceTransaction = serviceTransaction;
        this.serviceCategorie = serviceCategorie;
    }

    // ----- 2. Ajouter une dépense -----

    public void gererAjouterDepense() {
        if (!serviceCategorie.aCategorieActiveDeType(TypeTransaction.DEPENSE)) {
            vueTransaction.afficherAucuneCategorieActive(TypeTransaction.DEPENSE);
            return;
        }

        Categorie categorie = vueTransaction.demanderCategorie(serviceCategorie.getCategoriesActivesDeType(TypeTransaction.DEPENSE));
        double montant = vueTransaction.lireMontant("Montant de la dépense : ");
        LocalDate date = vueTransaction.lireDate("Date (JJ/MM/AAAA, vide = aujourd'hui) : ");
        String description = vueTransaction.demanderDescription("Description (facultative) : ");

        vueTransaction.afficherRecapitulatif(montant, categorie, date);

        // Règle de gestion "dépense > solde ⇒ avertissement" : le seuil est calculé par
        // ServicePortefeuille.depenseRendraSoldeNegatif(), pas ici. Le contrôleur ne fait que
        // brancher sur ce booléen, comme il le fait déjà pour ServiceEpargne.depasseraCible().
        if (servicePortefeuille.depenseRendraSoldeNegatif(montant)) {
            vueTransaction.afficherAvertissementSoldeNegatif(servicePortefeuille.soldeApresDepense(montant));
        }

        if (!vueTransaction.confirmer("Confirmer l'enregistrement de cette dépense ?")) {
            vueTransaction.afficherOperationAnnulee();
            return;
        }

        try {
            serviceTransaction.ajouterDepense(montant, categorie, date, description);
            vueTransaction.afficherDepenseEnregistree();
        } catch (ErreurSauvegardeException erreur) {
            confirmerNouvelleSauvegarde(erreur);
        }
    }

    // ----- 3. Ajouter un revenu -----

    public void gererAjouterRevenu() {
        if (!serviceCategorie.aCategorieActiveDeType(TypeTransaction.REVENU)) {
            vueTransaction.afficherAucuneCategorieActive(TypeTransaction.REVENU);
            return;
        }

        Categorie categorie = vueTransaction.demanderCategorie(serviceCategorie.getCategoriesActivesDeType(TypeTransaction.REVENU));
        double montant = vueTransaction.lireMontant("Montant du revenu : ");
        LocalDate date = vueTransaction.lireDate("Date (JJ/MM/AAAA, vide = aujourd'hui) : ");
        String description = vueTransaction.demanderDescription("Description (facultative) : ");

        vueTransaction.afficherRecapitulatif(montant, categorie, date);

        if (!vueTransaction.confirmer("Confirmer l'enregistrement de ce revenu ?")) {
            vueTransaction.afficherOperationAnnulee();
            return;
        }

        try {
            serviceTransaction.ajouterRevenu(montant, categorie, date, description);
            vueTransaction.afficherRevenuEnregistre();
        } catch (ErreurSauvegardeException erreur) {
            confirmerNouvelleSauvegarde(erreur);
        }
    }

    // ----- 4. Historique -----

    // Chaque branche appelle directement vueTransaction.afficherTransactions() avec son propre
    // résultat, plutôt que de le stocker dans une variable commune affichée après le switch : le
    // résultat ne doit pas être assemblé par le contrôleur, chaque branche se suffit à elle-même.
    public void gererHistorique() {
        vueTransaction.afficherMenuHistorique();
        int choix = vueTransaction.lireEntier("Votre choix : ");

        switch (choix) {
            case 1 -> vueTransaction.afficherTransactions(serviceTransaction.getHistorique());
            case 2 -> {
                LocalDate debut = vueTransaction.lireDate("Date de début (JJ/MM/AAAA) : ");
                LocalDate fin = vueTransaction.lireDate("Date de fin (JJ/MM/AAAA) : ");
                vueTransaction.afficherTransactions(serviceTransaction.filtrerParDate(debut, fin));
            }
            case 3 -> vueTransaction.afficherTransactions(serviceTransaction.filtrerParCategorie(vueTransaction.demanderCategorieParmiToutes()));
            case 4 -> vueTransaction.afficherTransactions(serviceTransaction.filtrerParType(vueTransaction.demanderType()));
            case 5 -> gererModificationSuppressionTransaction();
            default -> { }
        }
    }

    private void gererModificationSuppressionTransaction() {
        vueTransaction.afficherTransactions(serviceTransaction.getHistorique());
        int id = vueTransaction.lireEntier("Identifiant de la transaction à modifier/supprimer (0 pour annuler) : ");
        if (id == 0) {
            return;
        }

        vueTransaction.afficherMenuModifierSupprimer();
        int choix = vueTransaction.lireEntier("Votre choix : ");

        try {
            if (choix == 1) {
                // La catégorie proposée doit être active (règle de gestion "catégorie
                // cohérente") : contrairement au filtre (case 3), une catégorie inactive
                // choisie ici serait rejetée par IServiceTransaction, après coup, une fois
                // toutes les saisies déjà faites. On restreint donc aux catégories actives du
                // même type que la transaction existante.
                TypeTransaction type = serviceTransaction.getTransaction(id).getType();
                if (!serviceCategorie.aCategorieActiveDeType(type)) {
                    vueTransaction.afficherAucuneCategorieActive(type);
                    return;
                }

                double montant = vueTransaction.lireMontant("Nouveau montant : ");
                LocalDate date = vueTransaction.lireDate("Nouvelle date (JJ/MM/AAAA, vide = aujourd'hui) : ");
                Categorie categorie = vueTransaction.demanderCategorie(serviceCategorie.getCategoriesActivesDeType(type));
                String description = vueTransaction.demanderDescription("Nouvelle description (facultative) : ");
                serviceTransaction.modifierTransaction(id, montant, categorie, date, description);
                vueTransaction.afficherTransactionModifiee();
            } else if (choix == 2) {
                if (vueTransaction.confirmer("Confirmer la suppression de cette transaction ?")) {
                    serviceTransaction.supprimerTransaction(id);
                    vueTransaction.afficherTransactionSupprimee();
                } else {
                    vueTransaction.afficherOperationAnnulee();
                }
            }
        } catch (ErreurSauvegardeException erreur) {
            confirmerNouvelleSauvegarde(erreur);
        } catch (IllegalArgumentException | IllegalStateException erreur) {
            vueTransaction.afficherErreur(erreur.getMessage());
        }
    }
}
