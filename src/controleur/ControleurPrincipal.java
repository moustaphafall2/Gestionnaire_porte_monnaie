package controleur;

import metier.ErreurSauvegardeException;
import modele.service.ServicePortefeuille;
import vue.VuePrincipale;

/*
    * ControleurPrincipal tient la boucle du menu principal : il lit le choix de l'utilisateur
    * et aiguille vers l'écran correspondant. Il n'affiche jamais rien lui-même — chaque
    * affichage passe par VuePrincipale, à qui il transmet les valeurs obtenues des services.
    * Tous les écrans délèguent maintenant directement à leur contrôleur dédié : la migration
    * amorcée avec ControleurTransaction est terminée pour les sept écrans du menu principal.
*/
public class ControleurPrincipal {
    private VuePrincipale vuePrincipale;
    private ServicePortefeuille servicePortefeuille;
    private ControleurTransaction controleurTransaction;
    private ControleurEpargne controleurEpargne;
    private ControleurCategorie controleurCategorie;
    private ControleurStatistique controleurStatistique;

    public ControleurPrincipal(VuePrincipale vuePrincipale, ServicePortefeuille servicePortefeuille,
            ControleurTransaction controleurTransaction, ControleurEpargne controleurEpargne,
            ControleurCategorie controleurCategorie, ControleurStatistique controleurStatistique) {
        this.vuePrincipale = vuePrincipale;
        this.servicePortefeuille = servicePortefeuille;
        this.controleurTransaction = controleurTransaction;
        this.controleurEpargne = controleurEpargne;
        this.controleurCategorie = controleurCategorie;
        this.controleurStatistique = controleurStatistique;
    }

    // Boucle principale : affiche le menu, lit le choix, exécute l'action correspondante,
    // jusqu'à ce que l'utilisateur choisisse "Quitter".
    public void lancer() {
        boolean continuer = true;

        while (continuer) {
            vuePrincipale.afficherMenuPrincipal();
            int choix = vuePrincipale.lireEntier("Votre choix : ");

            // Attrapé ici, au niveau le plus haut, pour couvrir toutes les opérations d'un
            // coup : quelle que soit l'action en cours, un échec d'écriture disque ne doit
            // jamais faire planter l'application. Les données restent valides en mémoire
            // pour la suite de la session (l'opération elle-même a déjà eu lieu avant l'échec
            // de la sauvegarde), seule l'écriture sur le disque a échoué.
            try {
                switch (choix) {
                    case 1 -> gererVoirSolde();
                    case 2 -> controleurTransaction.gererAjouterDepense();
                    case 3 -> controleurTransaction.gererAjouterRevenu();
                    case 4 -> controleurTransaction.gererHistorique();
                    case 5 -> controleurEpargne.gererObjectifsEpargne();
                    case 6 -> controleurCategorie.gererCategories();
                    case 7 -> controleurStatistique.gererStatistiques();
                    case 8 -> continuer = false;
                    default -> vuePrincipale.afficherChoixInvalide();
                }
            } catch (ErreurSauvegardeException erreur) {
                vuePrincipale.afficherEchecSauvegarde(erreur.getMessage());
            }
        }

        vuePrincipale.afficherAuRevoir();
    }

    // ----- 1. Solde -----

    private void gererVoirSolde() {
        double soldeDisponible = servicePortefeuille.getSoldeDisponible();
        double totalEpargne = servicePortefeuille.getTotalEpargne();
        vuePrincipale.afficherSolde(soldeDisponible, totalEpargne);
    }
}
