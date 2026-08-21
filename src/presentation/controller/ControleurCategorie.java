package presentation.controller;

import domain.enumeration.Categorie;
import exception.ErreurSauvegardeException;
import application.service.interfaces.IServiceCategorie;
import application.service.interfaces.IServicePortefeuille;
import presentation.view.VueCategorie;

/*
    * ControleurCategorie enchaîne l'écran "Gérer mes catégories" : il lit les saisies via
    * VueCategorie, applique les règles en appelant ServiceCategorie (catégories disponibles,
    * activation, désactivation), et transmet le résultat à la vue. Il n'affiche jamais rien
    * lui-même et ne contient aucun calcul métier.
    *
    * Activer ou désactiver une catégorie modifie réellement les données du portefeuille. Si la
    * sauvegarde échoue après une opération déjà appliquée en mémoire (voir
    * ErreurSauvegardeException), le contrôleur propose à l'utilisateur de réessayer l'écriture
    * sur le disque, tant qu'il l'accepte, sans jamais rejouer l'opération elle-même. S'il
    * refuse, l'application continue normalement : ce n'est pas bloquant.
*/
public class ControleurCategorie extends ControleurConsole {
    private VueCategorie vueCategorie;
    private IServiceCategorie serviceCategorie;

    public ControleurCategorie(VueCategorie vueCategorie, IServiceCategorie serviceCategorie, IServicePortefeuille servicePortefeuille) {
        super(vueCategorie, servicePortefeuille);
        this.vueCategorie = vueCategorie;
        this.serviceCategorie = serviceCategorie;
    }

    // ----- 6. Catégories -----

    public void gererCategories() {
        vueCategorie.afficherCategoriesActives(serviceCategorie.getCategoriesActives());
        vueCategorie.afficherMenuCategories();
        int choix = vueCategorie.lireEntier("Votre choix : ");

        try {
            if (choix == 1) {
                gererActivationCategorie();
            } else if (choix == 2) {
                gererDesactivationCategorie();
            }
        } catch (ErreurSauvegardeException erreur) {
            confirmerNouvelleSauvegarde(erreur);
        }
    }

    // Traitement déplacé vers VueCategorie.demanderCategorieActivation() : vérifier si la liste
    // des catégories disponibles est vide relève de l'affichage (quel message montrer), pas
    // d'une règle métier. Le contrôleur ne fait plus qu'appeler un service, demander un choix
    // à la vue, et réagir à null (rien à activer) sans jamais inspecter la liste lui-même.
    private void gererActivationCategorie() {
        Categorie categorie = vueCategorie.demanderCategorieActivation(serviceCategorie.getCategoriesDisponibles());
        if (categorie == null) {
            return;
        }
        serviceCategorie.activerCategorie(categorie);
        vueCategorie.afficherCategorieActivee();
    }

    // Même principe que gererActivationCategorie() : la vue absorbe le test de liste vide et la
    // conversion Set -> List (nécessaire pour numéroter les catégories), le contrôleur se
    // contente d'enchaîner les appels.
    private void gererDesactivationCategorie() {
        Categorie categorie = vueCategorie.demanderCategorieDesactivation(serviceCategorie.getCategoriesActives());
        if (categorie == null) {
            return;
        }
        serviceCategorie.desactiverCategorie(categorie);
        vueCategorie.afficherCategorieDesactivee();
    }
}
