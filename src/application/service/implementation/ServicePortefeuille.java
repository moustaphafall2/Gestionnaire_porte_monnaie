package application.service.implementation;

import domain.entity.Portefeuille;

/*
    * ServicePortefeuille n'a plus qu'une responsabilité : détenir le Portefeuille en mémoire et
    * l'exposer aux autres services de application.service.implementation par une méthode à
    * visibilité de paquet. Depuis l'étape repository, il ne relaie plus aucune écriture vers la
    * base : chaque service (ServiceTransaction, ServiceEpargne, ServiceCategorie) parle
    * directement à son propre repository (TransactionRepository, EpargneRepository,
    * CategorieRepository). Les huit méthodes enregistrerXxx() qui existaient ici ont disparu
    * avec leur dernier appelant, en même temps que PortefeuilleRepository, qu'elles relayaient.
    *
    * Il continue d'exister pour une seule raison, non négociable : empêcher qu'un contrôleur
    * détienne une référence sur Portefeuille (règle de vérification n°4 du cahier des charges).
    * getDonnees() reste à visibilité de paquet, invisible depuis presentation.controller et
    * presentation.view.
*/
public class ServicePortefeuille {
    private final Portefeuille portefeuille;

    public ServicePortefeuille(Portefeuille portefeuille) {
        this.portefeuille = portefeuille;
    }

    // Visibilité de paquet, volontairement : seuls les services de application.service.implementation (le même
    // paquet) peuvent appeler cette méthode. Un contrôleur ou une vue, dans un autre paquet,
    // ne peut pas y accéder — le compilateur refuse la compilation s'il essaie. C'est ce qui
    // empêche de contourner les services pour manipuler le Portefeuille directement.
    Portefeuille getDonnees() {
        return portefeuille;
    }
}
