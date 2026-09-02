import presentation.controller.ControleurCategorie;
import presentation.controller.ControleurEpargne;
import presentation.controller.ControleurPortefeuille;
import presentation.controller.ControleurStatistique;
import presentation.controller.ControleurTransaction;
import domain.entity.Portefeuille;
import exception.ErreurChargementException;
import exception.ErreurSauvegardeException;
import infrastructure.persistence.CategorieRepository;
import infrastructure.persistence.ChargeurPortefeuille;
import infrastructure.persistence.EpargneRepository;
import infrastructure.persistence.TransactionRepository;
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
    * Point d'entrée du programme : initialise les repositories, le portefeuille, les services,
    * les vues et les contrôleurs, tient la boucle du menu principal et aiguille chaque choix vers
    * le contrôleur concerné. Seul endroit du projet où ces dépendances sont reliées entre elles.
*/
public class Main {
    public static void main(String[] args) {
        CategorieRepository categorieRepository = new CategorieRepository();
        TransactionRepository transactionRepository = new TransactionRepository();
        EpargneRepository epargneRepository = new EpargneRepository();
        ChargeurPortefeuille chargeurPortefeuille = new ChargeurPortefeuille(categorieRepository, transactionRepository, epargneRepository);
        VuePrincipale vuePrincipale = new VuePrincipale();

        Portefeuille portefeuille;
        try {
            portefeuille = chargeurPortefeuille.charger();
        } catch (ErreurChargementException erreur) {
            vuePrincipale.afficherErreur(erreur.getMessage());
            return;
        }

        ServicePortefeuille servicePortefeuille = new ServicePortefeuille(portefeuille);
        ServiceSolde serviceSolde = new ServiceSolde(servicePortefeuille);
        ServiceCategorie serviceCategorie = new ServiceCategorie(servicePortefeuille, categorieRepository);
        ServiceTransaction serviceTransaction = new ServiceTransaction(servicePortefeuille, serviceCategorie, transactionRepository);
        ServiceEpargne serviceEpargne = new ServiceEpargne(servicePortefeuille, serviceSolde, epargneRepository);
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
                vuePrincipale.afficherErreur(erreur.getMessage());
            }
        }

        vuePrincipale.afficherAuRevoir();
    }
}
