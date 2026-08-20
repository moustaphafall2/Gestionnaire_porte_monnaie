package controleur;

import java.util.List;

import metier.ErreurSauvegardeException;
import modele.enumeration.Categorie;
import modele.service.ServiceCategorie;
import modele.service.ServicePortefeuille;
import vue.VueCategorie;

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
public class ControleurCategorie {
    private VueCategorie vueCategorie;
    private ServiceCategorie serviceCategorie;
    private ServicePortefeuille servicePortefeuille;

    public ControleurCategorie(VueCategorie vueCategorie, ServiceCategorie serviceCategorie, ServicePortefeuille servicePortefeuille) {
        this.vueCategorie = vueCategorie;
        this.serviceCategorie = serviceCategorie;
        this.servicePortefeuille = servicePortefeuille;
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

    private void gererActivationCategorie() {
        List<Categorie> disponibles = serviceCategorie.getCategoriesDisponibles();
        if (disponibles.isEmpty()) {
            vueCategorie.afficherToutesActives();
            return;
        }

        Categorie categorie = vueCategorie.demanderCategorie(disponibles, "Numéro de la catégorie à activer : ");
        serviceCategorie.activerCategorie(categorie);
        vueCategorie.afficherCategorieActivee();
    }

    private void gererDesactivationCategorie() {
        List<Categorie> actives = List.copyOf(serviceCategorie.getCategoriesActives());
        if (actives.isEmpty()) {
            vueCategorie.afficherAucuneActive();
            return;
        }

        Categorie categorie = vueCategorie.demanderCategorie(actives, "Numéro de la catégorie à désactiver : ");
        serviceCategorie.desactiverCategorie(categorie);
        vueCategorie.afficherCategorieDesactivee();
    }

    // Réessaie uniquement l'écriture sur le disque, jamais l'opération elle-même : elle a déjà
    // eu lieu en mémoire au moment où ServiceCategorie lève cette exception (voir
    // ServicePortefeuille.sauvegarder()). Tant que l'utilisateur accepte de réessayer, on
    // rappelle directement servicePortefeuille.sauvegarder() ; s'il refuse, l'application
    // continue sans bloquer, avec un message clair sur les données non encore enregistrées.
    private void confirmerNouvelleSauvegarde(ErreurSauvegardeException erreur) {
        String messageErreur = erreur.getMessage();
        while (vueCategorie.demanderNouvelleTentativeSauvegarde(messageErreur)) {
            try {
                servicePortefeuille.sauvegarder();
                vueCategorie.afficherSauvegardeReussie();
                return;
            } catch (ErreurSauvegardeException nouvelleErreur) {
                messageErreur = nouvelleErreur.getMessage();
            }
        }
        vueCategorie.afficherSauvegardeAbandonnee();
    }
}
