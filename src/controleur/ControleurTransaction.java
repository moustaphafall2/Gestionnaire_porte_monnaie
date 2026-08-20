package controleur;

import java.time.LocalDate;
import java.util.List;

import metier.ErreurSauvegardeException;
import modele.entite.Transaction;
import modele.enumeration.Categorie;
import modele.enumeration.TypeTransaction;
import modele.service.ServiceCategorie;
import modele.service.ServicePortefeuille;
import modele.service.ServiceTransaction;
import vue.VueTransaction;

/*
    * ControleurTransaction enchaîne les écrans "Ajouter une dépense", "Ajouter un revenu" et
    * "Voir l'historique des transactions" (consultation, filtres, modification, suppression) :
    * il lit les saisies via VueTransaction, applique les règles en appelant ServiceCategorie
    * (catégorie active), ServicePortefeuille (solde) et ServiceTransaction (enregistrement,
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
public class ControleurTransaction {
    private VueTransaction vueTransaction;
    private ServiceTransaction serviceTransaction;
    private ServiceCategorie serviceCategorie;
    private ServicePortefeuille servicePortefeuille;

    public ControleurTransaction(VueTransaction vueTransaction, ServiceTransaction serviceTransaction,
            ServiceCategorie serviceCategorie, ServicePortefeuille servicePortefeuille) {
        this.vueTransaction = vueTransaction;
        this.serviceTransaction = serviceTransaction;
        this.serviceCategorie = serviceCategorie;
        this.servicePortefeuille = servicePortefeuille;
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

        // Règle de gestion : une dépense supérieure au solde disponible est autorisée, avec un
        // simple avertissement (une dépense passée est un fait).
        double soldeApres = servicePortefeuille.soldeApresDepense(montant);
        if (soldeApres < 0) {
            vueTransaction.afficherAvertissementSoldeNegatif(soldeApres);
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

    public void gererHistorique() {
        vueTransaction.afficherMenuHistorique();
        int choix = vueTransaction.lireEntier("Votre choix : ");

        List<Transaction> resultat;
        switch (choix) {
            case 1 -> resultat = serviceTransaction.getHistorique();
            case 2 -> {
                LocalDate debut = vueTransaction.lireDate("Date de début (JJ/MM/AAAA) : ");
                LocalDate fin = vueTransaction.lireDate("Date de fin (JJ/MM/AAAA) : ");
                resultat = serviceTransaction.filtrerParDate(debut, fin);
            }
            case 3 -> resultat = serviceTransaction.filtrerParCategorie(vueTransaction.demanderCategorieParmiToutes());
            case 4 -> resultat = serviceTransaction.filtrerParType(vueTransaction.demanderType());
            case 5 -> {
                gererModificationSuppressionTransaction();
                return;
            }
            default -> {
                return;
            }
        }

        vueTransaction.afficherTransactions(resultat);
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
                // choisie ici serait rejetée par ServiceTransaction, après coup, une fois
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

    // Réessaie uniquement l'écriture sur le disque, jamais l'opération elle-même : elle a déjà
    // eu lieu en mémoire au moment où ServiceTransaction lève cette exception (voir
    // ServicePortefeuille.sauvegarder()). Tant que l'utilisateur accepte de réessayer, on
    // rappelle directement servicePortefeuille.sauvegarder() ; s'il refuse, l'application
    // continue sans bloquer, avec un message clair sur les données non encore enregistrées.
    private void confirmerNouvelleSauvegarde(ErreurSauvegardeException erreur) {
        String messageErreur = erreur.getMessage();
        while (vueTransaction.demanderNouvelleTentativeSauvegarde(messageErreur)) {
            try {
                servicePortefeuille.sauvegarder();
                vueTransaction.afficherSauvegardeReussie();
                return;
            } catch (ErreurSauvegardeException nouvelleErreur) {
                messageErreur = nouvelleErreur.getMessage();
            }
        }
        vueTransaction.afficherSauvegardeAbandonnee();
    }
}
