package presentation;

import controleur.ControleurCategorie;
import controleur.ControleurEpargne;
import controleur.ControleurPrincipal;
import controleur.ControleurTransaction;
import modele.entite.Portefeuille;
import modele.service.ServiceCategorie;
import modele.service.ServiceEpargne;
import modele.service.ServicePortefeuille;
import modele.service.ServiceTransaction;
import persistance.GestionnaireFichier;
import vue.VueCategorie;
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
    * principal, écran "voir le solde"), ControleurTransaction (dépense/revenu/historique),
    * ControleurEpargne (objectifs d'épargne) et ControleurCategorie (catégories) existent pour
    * l'instant. Le contrôleur des statistiques sera relié ici à son tour.
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
        VueCategorie vueCategorie = new VueCategorie();
        ControleurTransaction controleurTransaction = new ControleurTransaction(vueTransaction, serviceTransaction,
                serviceCategorie, servicePortefeuille);
        ControleurEpargne controleurEpargne = new ControleurEpargne(vueEpargne, serviceEpargne, servicePortefeuille);
        ControleurCategorie controleurCategorie = new ControleurCategorie(vueCategorie, serviceCategorie, servicePortefeuille);
        ControleurPrincipal controleurPrincipal = new ControleurPrincipal(vuePrincipale, servicePortefeuille,
                controleurTransaction, controleurEpargne, controleurCategorie);
        controleurPrincipal.lancer();
    }
}
