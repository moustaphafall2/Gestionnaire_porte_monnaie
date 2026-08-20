package presentation;

import controleur.ControleurPrincipal;
import controleur.ControleurTransaction;
import modele.entite.Portefeuille;
import modele.service.ServiceCategorie;
import modele.service.ServicePortefeuille;
import modele.service.ServiceTransaction;
import persistance.GestionnaireFichier;
import vue.VuePrincipale;
import vue.VueTransaction;

/*
    * Point d'entrée du programme. Son unique rôle est d'initialiser les objets nécessaires
    * (GestionnaireFichier, Portefeuille, les services, les vues, les contrôleurs) et de
    * démarrer la boucle principale. C'est ici, et seulement ici, que les dépendances entre
    * services et contrôleurs sont reliées.
    *
    * Migration en cours vers l'architecture vue/contrôleur : ControleurPrincipal (menu
    * principal, écran "voir le solde") et ControleurTransaction (ajout d'une dépense/d'un
    * revenu) existent pour l'instant. Les services et contrôleurs des quatre autres écrans
    * seront reliés ici au fur et à mesure de leur migration.
*/
public class Main {
    public static void main(String[] args) {
        GestionnaireFichier gestionnaireFichier = new GestionnaireFichier("portefeuille.json");
        Portefeuille portefeuille = gestionnaireFichier.charger();
        ServicePortefeuille servicePortefeuille = new ServicePortefeuille(portefeuille, gestionnaireFichier);
        ServiceCategorie serviceCategorie = new ServiceCategorie(servicePortefeuille);
        ServiceTransaction serviceTransaction = new ServiceTransaction(servicePortefeuille, serviceCategorie);

        VuePrincipale vuePrincipale = new VuePrincipale();
        VueTransaction vueTransaction = new VueTransaction();
        ControleurTransaction controleurTransaction = new ControleurTransaction(vueTransaction, serviceTransaction,
                serviceCategorie, servicePortefeuille);
        ControleurPrincipal controleurPrincipal = new ControleurPrincipal(vuePrincipale, servicePortefeuille, controleurTransaction);
        controleurPrincipal.lancer();
    }
}
