package application.service.implementation;

import java.time.LocalDate;

import domain.entity.Portefeuille;
import domain.enumeration.Categorie;
import domain.enumeration.SensMouvement;
import domain.enumeration.TypeTransaction;
import application.service.interfaces.IServicePortefeuille;
import infrastructure.persistence.PortefeuilleRepository;

/*
    * ServicePortefeuille a deux responsabilités, et pas une de plus : détenir le Portefeuille, et
    * transmettre à PortefeuilleRepository chaque mutation déjà validée par les autres services.
    * Le calcul du solde disponible et du total épargné vit dans ServiceSolde (étape 5) ; la façon
    * dont les données sont réellement stockées (PostgreSQL depuis cette étape) vit dans
    * PortefeuilleRepository, jamais ici.
    *
    * Chaque méthode enregistrerXxx() est un simple relais vers PortefeuilleRepository — jamais
    * l'endroit où la structure du Portefeuille en mémoire change : ça reste le rôle des méthodes
    * de Portefeuille elle-même (ajouterTransaction, retirerObjectif, activerCategorie...),
    * appelées par les autres services via getDonnees(). Nom volontairement différent de ces
    * méthodes structurelles (enregistrer, pas ajouter/retirer/activer) : les deux se ressemblent
    * mais ne font pas la même chose, l'une écrit sur la base, l'autre mute la mémoire.
    *
    * Ordre attendu chez l'appelant, systématique depuis cette étape : persister d'abord
    * (enregistrerXxx), muter la mémoire ensuite. Jamais l'inverse — voir le journal de
    * développement pour la raison (génération des identifiants par la base, disparition de la
    * boucle de reprise après échec de sauvegarde).
    *
    * À la place, getDonnees() (visibilité de paquet) laisse les autres services de
    * application.service.implementation manipuler directement le Portefeuille — mais seulement
    * eux, puisque les contrôleurs et les vues sont dans d'autres paquets et n'y ont pas accès.
*/
public class ServicePortefeuille implements IServicePortefeuille {
    private final Portefeuille portefeuille;
    private final PortefeuilleRepository portefeuilleRepository;

    public ServicePortefeuille(Portefeuille portefeuille, PortefeuilleRepository portefeuilleRepository) {
        this.portefeuille = portefeuille;
        this.portefeuilleRepository = portefeuilleRepository;
    }

    public void enregistrerActivationCategorie(Categorie categorie) {
        portefeuilleRepository.activerCategorie(categorie);
    }

    public void enregistrerDesactivationCategorie(Categorie categorie) {
        portefeuilleRepository.desactiverCategorie(categorie);
    }

    public int enregistrerNouvelleTransaction(double montant, TypeTransaction type, Categorie categorie, LocalDate date, String description) {
        return portefeuilleRepository.ajouterTransaction(montant, type, categorie, date, description);
    }

    public void enregistrerModificationTransaction(int id, double montant, Categorie categorie, LocalDate date, String description) {
        portefeuilleRepository.modifierTransaction(id, montant, categorie, date, description);
    }

    public void enregistrerSuppressionTransaction(int id) {
        portefeuilleRepository.supprimerTransaction(id);
    }

    public int enregistrerNouvelObjectif(String nom, double montantCible, LocalDate dateLimite) {
        return portefeuilleRepository.ajouterObjectif(nom, montantCible, dateLimite);
    }

    public void enregistrerNouveauMouvement(int idObjectif, double montant, SensMouvement sens, LocalDate date) {
        portefeuilleRepository.ajouterMouvement(idObjectif, montant, sens, date);
    }

    public void enregistrerSuppressionObjectif(int id) {
        portefeuilleRepository.supprimerObjectif(id);
    }

    // Visibilité de paquet, volontairement : seuls les services de application.service.implementation (le même
    // paquet) peuvent appeler cette méthode. Un contrôleur ou une vue, dans un autre paquet,
    // ne peut pas y accéder — le compilateur refuse la compilation s'il essaie. C'est ce qui
    // empêche de contourner les services pour manipuler le Portefeuille directement.
    Portefeuille getDonnees() {
        return portefeuille;
    }
}
