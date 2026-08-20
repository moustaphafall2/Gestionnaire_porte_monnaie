package presentation;

import controleur.ControleurEpargne;
import controleur.ControleurPrincipal;
import controleur.ControleurTransaction;
import modele.entite.Portefeuille;
import modele.service.ServiceCategorie;
import modele.service.ServiceEpargne;
import modele.service.ServicePortefeuille;
import modele.service.ServiceTransaction;
import persistance.GestionnaireFichier;
import vue.VueEpargne;
import vue.VuePrincipale;
import vue.VueTransaction;

/*
    * Point d'entrée du programme. Son unique rôle est d'initialiser les objets nécessaires
    * (GestionnaireFichier, Portefeuille, les services, les vues, les contrôleurs) et de
    * démarrer la boucle principale. C'est ici, et seulement ici, que les dépendances entre
    * services et contrôleurs sont reliées.
    *
    * Migration en cours vers l'architecture vue/contrôleur : ControleurPrincipal (menu
    * principal, écran "voir le solde"), ControleurTransaction (dépense/revenu/historique) et
    * ControleurEpargne (objectifs d'épargne) existent pour l'instant. Les services et
    * contrôleurs des deux autres écrans seront reliés ici au fur et à mesure de leur migration.
*/
public class Main {
    public static void main(String[] args) {
        GestionnaireFichier gestionnaireFichier = new GestionnaireFichier("portefeuille.json");
        Portefeuille portefeuille = gestionnaireFichier.charger();
        ServicePortefeuille servicePortefeuille = new ServicePortefeuille(portefeuille, gestionnaireFichier);
        ServiceCategorie serviceCategorie = new ServiceCategorie(servicePortefeuille);
        ServiceTransaction serviceTransaction = new ServiceTransaction(servicePortefeuille, serviceCategorie);
        ServiceEpargne serviceEpargne = new ServiceEpargne(servicePortefeuille);

        VuePrincipale vuePrincipale = new VuePrincipale();
        VueTransaction vueTransaction = new VueTransaction();
        VueEpargne vueEpargne = new VueEpargne();
        ControleurTransaction controleurTransaction = new ControleurTransaction(vueTransaction, serviceTransaction,
                serviceCategorie, servicePortefeuille);
        ControleurEpargne controleurEpargne = new ControleurEpargne(vueEpargne, serviceEpargne, servicePortefeuille);
        ControleurPrincipal controleurPrincipal = new ControleurPrincipal(vuePrincipale, servicePortefeuille,
                controleurTransaction, controleurEpargne);
        controleurPrincipal.lancer();
    }
}
