package presentation.controller;

import exception.ErreurSauvegardeException;
import application.service.interfaces.IServicePortefeuille;
import presentation.view.VueConsole;

/*
    * ControleurConsole rassemble ce qui est commun aux contrôleurs qui modifient réellement les
    * données du portefeuille (ControleurCategorie, ControleurTransaction, ControleurEpargne) :
    * la reprise après un échec de sauvegarde. Même principe que VueConsole côté vue : plutôt que
    * de recopier la même boucle dans chaque contrôleur, elle est écrite une seule fois ici et
    * héritée.
    *
    * Cette boucle a été examinée pour savoir si elle pouvait descendre plus bas, et la réponse
    * est non. Elle alterne deux natures d'appel à chaque tour : poser une question (la vue,
    * VueConsole.demanderNouvelleTentativeSauvegarde) et retenter l'écriture (le service,
    * ServicePortefeuille.sauvegarder), avec une décision différente selon ce que renvoie chacun.
    * - La faire porter par ServicePortefeuille obligerait le service à appeler une vue : une
    *   classe de application (couche service) qui déclenche un affichage/une saisie console, ce
    *   que la règle 1 du CLAUDE.md interdit déjà pour System.out.println/Scanner, et ce que le
    *   test "si je remplace la console par une interface graphique" condamnerait directement.
    * - La faire porter par la vue obligerait la vue à appeler un service, ce que la règle 2 du
    *   CLAUDE.md interdit sans exception.
    * - Confier "la retenter" au service via un objet passé en paramètre (un rappel/callback)
    *   éviterait ces deux dépendances, mais ça n'existe pas en Java de base sans lambda, classe
    *   anonyme ou interface fonctionnelle dédiée — exactement ce que le CLAUDE.md interdit sans
    *   accord préalable.
    * Seul un contrôleur a le droit d'appeler à la fois une vue et un service : cette boucle reste
    * donc ici, dans la couche contrôleur. Ce qui pouvait être amélioré l'a été (elle n'est plus
    * écrite trois fois) ; ce qui reste n'a pas d'autre endroit possible dans cette architecture.
*/
public class ControleurConsole {
    protected VueConsole vueConsole;
    protected IServicePortefeuille servicePortefeuille;

    protected ControleurConsole(VueConsole vueConsole, IServicePortefeuille servicePortefeuille) {
        this.vueConsole = vueConsole;
        this.servicePortefeuille = servicePortefeuille;
    }

    // Réessaie uniquement l'écriture sur le disque, jamais l'opération elle-même : elle a déjà
    // eu lieu en mémoire au moment où le service a levé cette exception (voir
    // ServicePortefeuille.sauvegarder()). Tant que l'utilisateur accepte de réessayer, on
    // rappelle directement servicePortefeuille.sauvegarder() ; s'il refuse, l'application
    // continue sans bloquer, avec un message clair sur les données non encore enregistrées.
    // Écrite une seule fois ici : ControleurCategorie, ControleurTransaction et
    // ControleurEpargne l'héritent et se contentent de l'appeler depuis leur bloc
    // catch (ErreurSauvegardeException), sans plus rien y écrire elles-mêmes.
    protected void confirmerNouvelleSauvegarde(ErreurSauvegardeException erreur) {
        String messageErreur = erreur.getMessage();
        while (vueConsole.demanderNouvelleTentativeSauvegarde(messageErreur)) {
            try {
                servicePortefeuille.sauvegarder();
                vueConsole.afficherSauvegardeReussie();
                return;
            } catch (ErreurSauvegardeException nouvelleErreur) {
                messageErreur = nouvelleErreur.getMessage();
            }
        }
        vueConsole.afficherSauvegardeAbandonnee();
    }
}
