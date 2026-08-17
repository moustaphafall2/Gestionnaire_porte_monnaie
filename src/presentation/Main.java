package presentation;

import metier.Portefeuille;
import persistance.GestionnaireFichier;

/*
    * Point d'entrée du programme. Son unique rôle est d'initialiser les objets nécessaires
    * (GestionnaireFichier, Portefeuille, Menu) et de démarrer la boucle principale.
*/
public class Main {
    public static void main(String[] args) {
        GestionnaireFichier gestionnaireFichier = new GestionnaireFichier("portefeuille.json");
        Portefeuille portefeuille = Portefeuille.charger(gestionnaireFichier);
        Menu menu = new Menu(portefeuille);
        menu.lancer();
    }
}
