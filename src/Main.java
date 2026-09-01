import presentation.controller.ControleurCategorie;
import presentation.controller.ControleurEpargne;
import presentation.controller.ControleurPortefeuille;
import presentation.controller.ControleurStatistique;
import presentation.controller.ControleurTransaction;
import domain.entity.Portefeuille;
import exception.ErreurChargementException;
import exception.ErreurSauvegardeException;
import infrastructure.persistence.GestionnairePostgreSQL;
import application.service.implementation.ServiceCategorie;
import application.service.implementation.ServiceEpargne;
import application.service.implementation.ServicePortefeuille;
import application.service.implementation.ServiceSolde;
import application.service.implementation.ServiceStatistique;
import application.service.implementation.ServiceTransaction;
import presentation.view.VueCategorie;
import presentation.view.VueEpargne;
import presentation.view.VuePrincipale;
import presentation.view.VueStatistique;
import presentation.view.VueTransaction;

/*
    * Point d'entrée du programme. Son unique rôle est d'initialiser les objets nécessaires
    * (GestionnairePostgreSQL, Portefeuille, les services, les vues, les contrôleurs), de tenir la
    * boucle du menu principal et d'aiguiller chaque choix vers le contrôleur concerné. C'est ici,
    * et seulement ici, que les dépendances entre services et contrôleurs sont reliées, et que
    * plusieurs contrôleurs différents sont appelés depuis un même endroit — un contrôleur, lui,
    * n'en appelle jamais un autre.
    *
    * Chaque vue affiche son propre menu ou sous-menu et renvoie directement le choix lu
    * (VuePrincipale.demanderChoix(), VueCategorie.demanderChoixMenu(),
    * VueEpargne.demanderChoixMenu(), VueTransaction.demanderChoixMenuHistorique()) : Main fait
    * son switch directement sur cette valeur, sans méthode intermédiaire ni le moindre texte à
    * lui, pour que tout le cheminement de l'application se lise ici, à un seul endroit.
    *
    * Depuis l'étape 6, un échec d'écriture (ErreurSauvegardeException) ne déclenche plus de
    * boucle de reprise : chaque service persiste avant de modifier la mémoire (voir
    * ServicePortefeuille), donc un échec ne laisse plus rien en suspens à rattraper — l'opération
    * n'a simplement pas eu lieu, ni en base ni en mémoire. Main se contente d'afficher l'erreur et
    * de reprendre la boucle du menu.
*/
public class Main {
    public static void main(String[] args) {
        GestionnairePostgreSQL portefeuilleRepository = new GestionnairePostgreSQL();
        VuePrincipale vuePrincipale = new VuePrincipale();

        // charger() absorbe déjà une base vide (premier lancement) en renvoyant un portefeuille
        // vide ; seule une vraie erreur de connexion ou de lecture lève ErreurChargementException.
        // Sans portefeuille valide, rien d'autre ne peut démarrer : on affiche un message lisible
        // et on arrête proprement, jamais de trace d'exception brute.
        Portefeuille portefeuille;
        try {
            portefeuille = portefeuilleRepository.charger();
        } catch (ErreurChargementException erreur) {
            vuePrincipale.afficherErreur(erreur.getMessage());
            return;
        }

        ServicePortefeuille servicePortefeuille = new ServicePortefeuille(portefeuille, portefeuilleRepository);
        ServiceSolde serviceSolde = new ServiceSolde(servicePortefeuille);
        ServiceCategorie serviceCategorie = new ServiceCategorie(servicePortefeuille);
        ServiceTransaction serviceTransaction = new ServiceTransaction(servicePortefeuille, serviceCategorie);
        ServiceEpargne serviceEpargne = new ServiceEpargne(servicePortefeuille, serviceSolde);
        ServiceStatistique serviceStatistique = new ServiceStatistique(serviceTransaction);

        VueTransaction vueTransaction = new VueTransaction();
        VueEpargne vueEpargne = new VueEpargne();
        VueCategorie vueCategorie = new VueCategorie();
        VueStatistique vueStatistique = new VueStatistique();

        ControleurPortefeuille controleurPortefeuille = new ControleurPortefeuille(vuePrincipale, serviceSolde);
        ControleurTransaction controleurTransaction = new ControleurTransaction(vueTransaction, serviceTransaction,
                serviceCategorie, serviceSolde);
        ControleurEpargne controleurEpargne = new ControleurEpargne(vueEpargne, serviceEpargne, serviceSolde);
        ControleurCategorie controleurCategorie = new ControleurCategorie(vueCategorie, serviceCategorie);
        ControleurStatistique controleurStatistique = new ControleurStatistique(vueStatistique, serviceStatistique);

        boolean continuer = true;

        while (continuer) {
            int choix = vuePrincipale.demanderChoix();

            // Attrapée ici, au niveau le plus haut, pour couvrir toutes les actions d'un coup :
            // quelle que soit celle en cours, un échec d'écriture disque ne doit jamais faire
            // planter l'application. Les données restent valides en mémoire pour la suite de la
            // session (l'opération elle-même a déjà eu lieu avant l'échec de la sauvegarde),
            // seule l'écriture sur le disque a échoué.
            try {
                switch (choix) {
                    case 1 -> controleurPortefeuille.afficherSolde();
                    case 2 -> controleurTransaction.ajouterDepense();
                    case 3 -> controleurTransaction.ajouterRevenu();
                    case 4 -> {
                        switch (vueTransaction.demanderChoixMenuHistorique()) {
                            case 1 -> controleurTransaction.afficherHistoriqueComplet();
                            case 2 -> controleurTransaction.afficherHistoriqueParDate();
                            case 3 -> controleurTransaction.afficherHistoriqueParCategorie();
                            case 4 -> controleurTransaction.afficherHistoriqueParType();
                            case 5 -> controleurTransaction.modifierTransaction();
                            case 6 -> controleurTransaction.supprimerTransaction();
                            default -> { }
                        }
                    }
                    case 5 -> {
                        switch (vueEpargne.demanderChoixMenu()) {
                            case 1 -> controleurEpargne.creerObjectif();
                            case 2 -> controleurEpargne.contribuerObjectif();
                            case 3 -> controleurEpargne.retirerObjectif();
                            case 4 -> controleurEpargne.afficherObjectifs();
                            case 5 -> controleurEpargne.supprimerObjectif();
                            default -> { }
                        }
                    }
                    case 6 -> {
                        // Seul appel de service fait directement par Main plutôt que par un
                        // contrôleur : afficher les catégories actives avant de proposer le
                        // sous-menu vivait dans ControleurCategorie.gererCategories(), qui a
                        // disparu avec le reste de l'aiguillage. Un simple getter, sans calcul.
                        vueCategorie.afficherCategoriesActives(serviceCategorie.getCategoriesActives());
                        switch (vueCategorie.demanderChoixMenu()) {
                            case 1 -> controleurCategorie.activerCategorie();
                            case 2 -> controleurCategorie.desactiverCategorie();
                            default -> { }
                        }
                    }
                    case 7 -> controleurStatistique.afficherStatistiques();
                    case 8 -> continuer = false;
                    default -> vuePrincipale.afficherChoixInvalide();
                }
            } catch (ErreurSauvegardeException erreur) {
                // Rien à rattraper : l'opération n'a pas été appliquée (voir ServicePortefeuille,
                // persister précède toujours muter la mémoire depuis cette étape). L'application
                // reprend simplement la boucle du menu.
                vuePrincipale.afficherErreur(erreur.getMessage());
            }
        }

        vuePrincipale.afficherAuRevoir();
    }
}
