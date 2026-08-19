package presentation;

import controleur.ControleurPrincipal;
import modele.entite.Portefeuille;
import modele.service.ServicePortefeuille;
import persistance.GestionnaireFichier;
import vue.VuePrincipale;

/*
    * Point d'entrée du programme. Son unique rôle est d'initialiser les objets nécessaires
    * (GestionnaireFichier, Portefeuille, les services, les vues, les contrôleurs) et de
    * démarrer la boucle principale. C'est ici, et seulement ici, que les dépendances entre
    * services et contrôleurs sont reliées.
    *
    * Migration en cours vers l'architecture vue/contrôleur : seul ControleurPrincipal existe
    * pour l'instant (menu principal, écran "voir le solde"). Les services et contrôleurs des
    * cinq autres écrans seront reliés ici au fur et à mesure de leur migration.
*/
public class Main {
    public static void main(String[] args) {
        GestionnaireFichier gestionnaireFichier = new GestionnaireFichier("portefeuille.json");
        Portefeuille portefeuille = gestionnaireFichier.charger();
        ServicePortefeuille servicePortefeuille = new ServicePortefeuille(portefeuille, gestionnaireFichier);

        VuePrincipale vuePrincipale = new VuePrincipale();
        ControleurPrincipal controleurPrincipal = new ControleurPrincipal(vuePrincipale, servicePortefeuille);
        controleurPrincipal.lancer();
    }
}
