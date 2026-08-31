package application.service.implementation;

import domain.entity.Portefeuille;
import application.service.interfaces.IServicePortefeuille;
import infrastructure.persistence.PortefeuilleRepository;

/*
    * ServicePortefeuille a deux responsabilités, et pas une de plus : détenir le Portefeuille, et
    * déclencher sa sauvegarde. Le calcul du solde disponible et du total épargné, qui vivait ici
    * jusqu'à l'étape 5, est parti dans ServiceSolde : ni de la détention ni de la persistance,
    * juste une lecture dérivée des mêmes données, utilisée uniquement par les contrôleurs et
    * ServiceEpargne — jamais par ServiceTransaction ni ServiceCategorie, qui n'ont besoin que de
    * détenir/sauvegarder, ni par ServiceStatistique, qui ne dépend même plus de cette classe
    * depuis cette même étape (voir ServiceStatistique).
    *
    * À la place, getDonnees() (visibilité de paquet) laisse les autres services de
    * application.service.implementation manipuler directement le Portefeuille — mais seulement eux, puisque
    * les contrôleurs et les vues sont dans d'autres paquets et n'y ont pas accès.
    *
    * La persistance passe par PortefeuilleRepository, jamais par GestionnaireFichier
    * directement : ce service ignore tout du stockage réel (fichier JSON aujourd'hui), il ne
    * connaît que charger()/sauvegarder(). Seul Main sait que l'implémentation concrète est
    * GestionnaireFichier.
*/
public class ServicePortefeuille implements IServicePortefeuille {
    private final Portefeuille portefeuille;
    private final PortefeuilleRepository portefeuilleRepository;

    public ServicePortefeuille(Portefeuille portefeuille, PortefeuilleRepository portefeuilleRepository) {
        this.portefeuille = portefeuille;
        this.portefeuilleRepository = portefeuilleRepository;
    }

    // Seul point d'écriture du portefeuille sur le disque. Les autres services appellent
    // cette méthode après chaque opération validée, plutôt que de détenir eux-mêmes
    // PortefeuilleRepository ou Portefeuille.
    public void sauvegarder() {
        portefeuilleRepository.sauvegarder(portefeuille);
    }

    // Visibilité de paquet, volontairement : seuls les services de application.service.implementation (le même
    // paquet) peuvent appeler cette méthode. Un contrôleur ou une vue, dans un autre paquet,
    // ne peut pas y accéder — le compilateur refuse la compilation s'il essaie. C'est ce qui
    // empêche de contourner les services pour manipuler le Portefeuille directement.
    Portefeuille getDonnees() {
        return portefeuille;
    }
}
