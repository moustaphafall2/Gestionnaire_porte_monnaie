import presentation.controller.ControleurCategorie;
import presentation.controller.ControleurEpargne;
import presentation.controller.ControleurPrincipal;
import presentation.controller.ControleurStatistique;
import presentation.controller.ControleurTransaction;
import domain.entity.Portefeuille;
import exception.ErreurChargementException;
import infrastructure.persistence.GestionnaireFichier;
import application.service.implementation.ServiceCategorie;
import application.service.implementation.ServiceEpargne;
import application.service.implementation.ServicePortefeuille;
import application.service.implementation.ServiceStatistique;
import application.service.implementation.ServiceTransaction;
import presentation.view.VueCategorie;
import presentation.view.VueEpargne;
import presentation.view.VuePrincipale;
import presentation.view.VueStatistique;
import presentation.view.VueTransaction;

/*
    * Point d'entrée du programme. Son unique rôle est d'initialiser les objets nécessaires
    * (GestionnaireFichier, Portefeuille, les services, les vues, les contrôleurs) et de
    * démarrer la boucle principale. C'est ici, et seulement ici, que les dépendances entre
    * services et contrôleurs sont reliées.
    *
    * Migration vue/contrôleur terminée : les sept écrans du menu principal ont chacun leur
    * contrôleur dédié (ControleurPrincipal ne garde que le menu et l'écran "voir le solde").
*/
public class Main {
    public static void main(String[] args) {
        GestionnaireFichier gestionnaireFichier = new GestionnaireFichier("portefeuille.json");
        VuePrincipale vuePrincipale = new VuePrincipale();

        // GestionnaireFichier.charger() absorbe déjà fichier absent/vide/JSON malformé en
        // renvoyant un portefeuille vide ; seule une vraie erreur de lecture disque (droits,
        // panne...) lève ErreurChargementException. Sans portefeuille valide, rien d'autre ne
        // peut démarrer : on affiche un message lisible et on arrête proprement, jamais de
        // trace d'exception brute.
        Portefeuille portefeuille;
        try {
            portefeuille = gestionnaireFichier.charger();
        } catch (ErreurChargementException erreur) {
            vuePrincipale.afficherErreur(erreur.getMessage());
            return;
        }

        ServicePortefeuille servicePortefeuille = new ServicePortefeuille(portefeuille, gestionnaireFichier);
        ServiceCategorie serviceCategorie = new ServiceCategorie(servicePortefeuille);
        ServiceTransaction serviceTransaction = new ServiceTransaction(servicePortefeuille, serviceCategorie);
        ServiceEpargne serviceEpargne = new ServiceEpargne(servicePortefeuille);
        ServiceStatistique serviceStatistique = new ServiceStatistique(servicePortefeuille);

        VueTransaction vueTransaction = new VueTransaction();
        VueEpargne vueEpargne = new VueEpargne();
        VueCategorie vueCategorie = new VueCategorie();
        VueStatistique vueStatistique = new VueStatistique();
        ControleurTransaction controleurTransaction = new ControleurTransaction(vueTransaction, serviceTransaction,
                serviceCategorie, servicePortefeuille);
        ControleurEpargne controleurEpargne = new ControleurEpargne(vueEpargne, serviceEpargne, servicePortefeuille);
        ControleurCategorie controleurCategorie = new ControleurCategorie(vueCategorie, serviceCategorie, servicePortefeuille);
        ControleurStatistique controleurStatistique = new ControleurStatistique(vueStatistique, serviceStatistique);
        ControleurPrincipal controleurPrincipal = new ControleurPrincipal(vuePrincipale, servicePortefeuille,
                controleurTransaction, controleurEpargne, controleurCategorie, controleurStatistique);
        controleurPrincipal.lancer();
    }
}
