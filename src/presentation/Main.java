package presentation;

import modele.entite.Portefeuille;
import modele.service.ServiceCategorie;
import modele.service.ServiceEpargne;
import modele.service.ServicePortefeuille;
import modele.service.ServiceTransaction;
import persistance.GestionnaireFichier;

/*
    * Point d'entrée du programme. Son unique rôle est d'initialiser les objets nécessaires
    * (GestionnaireFichier, Portefeuille, les services, Menu) et de démarrer la boucle
    * principale. C'est ici, et seulement ici, que les dépendances entre services sont reliées.
*/
public class Main {
    public static void main(String[] args) {
        GestionnaireFichier gestionnaireFichier = new GestionnaireFichier("portefeuille.json");
        Portefeuille portefeuille = gestionnaireFichier.charger();
        ServicePortefeuille servicePortefeuille = new ServicePortefeuille(portefeuille, gestionnaireFichier);
        ServiceEpargne serviceEpargne = new ServiceEpargne(servicePortefeuille);
        ServiceCategorie serviceCategorie = new ServiceCategorie(servicePortefeuille);
        ServiceTransaction serviceTransaction = new ServiceTransaction(servicePortefeuille, serviceCategorie);
        Menu menu = new Menu(portefeuille, servicePortefeuille, serviceEpargne, serviceTransaction);
        menu.lancer();
    }
}
