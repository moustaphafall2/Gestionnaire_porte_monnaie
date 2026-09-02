package application.service.implementation;

import domain.entity.Portefeuille;

/*
    * ServicePortefeuille détient le Portefeuille en mémoire et l'expose aux services du même
    * paquet par getDonnees(). Aucun contrôleur n'y a accès.
*/
public class ServicePortefeuille {
    private final Portefeuille portefeuille;

    public ServicePortefeuille(Portefeuille portefeuille) {
        this.portefeuille = portefeuille;
    }

    // Visibilité de paquet volontaire : un contrôleur, dans un autre paquet, ne peut pas y
    // accéder, le compilateur refuse la compilation s'il essaie.
    Portefeuille getDonnees() {
        return portefeuille;
    }
}
