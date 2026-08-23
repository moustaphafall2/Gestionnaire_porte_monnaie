package infrastructure.persistence;

import domain.entity.Portefeuille;

/*
    * PortefeuilleRepository est le seul contrat que la couche application connaît de la
    * persistance : charger un portefeuille, en sauvegarder un. ServicePortefeuille ne dépend
    * que de cette interface, jamais de GestionnaireFichier ni de la façon dont les données
    * sont réellement stockées (fichier JSON aujourd'hui, autre chose demain sans que
    * ServicePortefeuille ait à changer).
*/
public interface PortefeuilleRepository {
    public Portefeuille charger();
    public void sauvegarder(Portefeuille portefeuille);
}
