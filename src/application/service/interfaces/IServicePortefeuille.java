package application.service.interfaces;

/*
    * Vide depuis le branchement final de l'étape 6 : sauvegarder() a disparu avec la boucle de
    * reprise après échec de Main, qui n'a plus de sens depuis que l'ordre "persister d'abord,
    * muter la mémoire ensuite" ne laisse plus jamais rien à rattraper. Plus aucun consommateur
    * hors du paquet application.service.implementation ne dépend de ServicePortefeuille par une
    * interface (ServicePortefeuille continue de l'implémenter, sans le supprimer). Signalé plutôt
    * que supprimé : décision laissée à l'étudiant.
*/
public interface IServicePortefeuille {
}
