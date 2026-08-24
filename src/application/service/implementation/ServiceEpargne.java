package application.service.implementation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import domain.entity.Epargne;
import domain.entity.MouvementEpargne;
import domain.entity.Portefeuille;
import domain.enumeration.SensMouvement;
import application.service.interfaces.IServiceEpargne;

/*
    * ServiceEpargne porte désormais toutes les règles de gestion des objectifs d'épargne :
    * création, contribution, retrait, suppression, et les calculs de progression (montant
    * actuel, pourcentage atteint, cible dépassée...). Cette logique vivait auparavant dans
    * Epargne et Portefeuille ; Epargne ne garde plus que sa structure (attributs, constructeur,
    * getters, ajouterMouvement), et Portefeuille se limite à l'accès par identifiant
    * (getObjectif), un accès par clé et non un calcul.
    *
    * Il porte aussi, depuis cette étape, toute la validation qui vivait auparavant dans Epargne
    * (nom non vide, montant cible strictement positif) et dans MouvementEpargne (montant
    * strictement positif, sens non nul, date pas dans le futur) : aucune des deux entités ne se
    * protège plus elle-même, c'est ce service qui garantit qu'aucun objet invalide ne peut être
    * construit.
    *
    * Comme les autres services, il ne détient jamais Portefeuille directement : il passe par
    * servicePortefeuille.getDonnees() pour lire ou modifier les objectifs, et par
    * sauvegarder() pour écrire le résultat.
*/
public class ServiceEpargne implements IServiceEpargne {
    // Les montants sont des FCFA sans centimes, mais restent des double : deux montants
    // "égaux" en théorie peuvent différer d'une poussière après une suite d'additions/
    // soustractions. On compare donc à un epsilon près plutôt qu'avec ==.
    private static final double EPSILON = 0.01;

    private final ServicePortefeuille servicePortefeuille;

    public ServiceEpargne(ServicePortefeuille servicePortefeuille) {
        this.servicePortefeuille = servicePortefeuille;
    }

    // Montant actuellement épargné sur cet objectif. Le calcul passe par CalculEpargne, partagé
    // avec ServicePortefeuille (getTotalEpargne), pour qu'il n'existe qu'à un seul endroit.
    public double getMontantActuel(Epargne objectif) {
        return CalculEpargne.calculerMontantActuel(objectif);
    }

    public double getPourcentageAtteint(Epargne objectif) {
        return (getMontantActuel(objectif) / objectif.getMontantCible()) * 100;
    }

    // Calcule le montant actuel de toute une liste d'objectifs d'un coup. Le contrôleur affichait
    // auparavant chaque ligne dans une boucle qui appelait getMontantActuel() objectif par
    // objectif : itérer pour calculer est un traitement, ce n'est pas le rôle d'un contrôleur.
    // La boucle est ici, dans le service, à côté du calcul qu'elle répète.
    public List<Double> getMontantsActuels(List<Epargne> objectifs) {
        List<Double> montants = new ArrayList<>();
        for (Epargne objectif : objectifs) {
            montants.add(getMontantActuel(objectif));
        }
        return montants;
    }

    // Même principe que getMontantsActuels(), pour le pourcentage atteint.
    public List<Double> getPourcentagesAtteints(List<Epargne> objectifs) {
        List<Double> pourcentages = new ArrayList<>();
        for (Epargne objectif : objectifs) {
            pourcentages.add(getPourcentageAtteint(objectif));
        }
        return pourcentages;
    }

    // Indique si une contribution de ce montant ferait dépasser le montant cible. Dépasser la
    // cible reste autorisé (règle de gestion) : c'est à l'appelant (ControleurEpargne) de
    // décider s'il signale ce dépassement avant de confirmer l'opération.
    public boolean depasseraCible(Epargne objectif, double montant) {
        return getMontantActuel(objectif) + montant > objectif.getMontantCible();
    }

    // Condition nécessaire à la suppression de l'objectif
    private boolean estVide(Epargne objectif) {
        return Math.abs(getMontantActuel(objectif)) < EPSILON;
    }

    public boolean estAtteint(Epargne objectif) {
        return getMontantActuel(objectif) >= objectif.getMontantCible();
    }

    // Liste complète des objectifs, utilisée par ControleurEpargne pour les afficher (avec leur
    // progression, calculée à part par getMontantActuel/getPourcentageAtteint) avant de demander
    // un identifiant à l'utilisateur.
    public List<Epargne> getObjectifs() {
        return servicePortefeuille.getDonnees().getObjectifs();
    }

    // Recherche publique d'un objectif par id, utilisée par ControleurEpargne pour récupérer
    // l'objectif choisi (nom, mouvements...) avant de contribuer, retirer ou en afficher le
    // détail.
    public Epargne getObjectif(int idObjectif) {
        return trouverObjectif(idObjectif);
    }

    // Anciennement Portefeuille.getObjectif()/trouverObjectif() : une recherche par identifiant
    // est un parcours de liste, donc un traitement, jamais un accès à un attribut. Même principe
    // que ServiceTransaction.trouverTransaction(), qui fait exactement ça pour les transactions
    // sans jamais passer par une méthode de recherche de Portefeuille.
    private Epargne trouverObjectif(int idObjectif) {
        for (Epargne objectif : servicePortefeuille.getDonnees().getObjectifs()) {
            if (objectif.getId() == idObjectif) {
                return objectif;
            }
        }
        throw new IllegalArgumentException("Aucun objectif avec l'identifiant " + idObjectif + ".");
    }

    // Anciennement Epargne.validerNom().
    private void validerNomObjectif(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom de l'objectif est obligatoire.");
        }
    }

    // Anciennement Epargne.validerMontantCible().
    private void validerMontantCible(double montantCible) {
        if (montantCible <= 0) {
            throw new IllegalArgumentException("Le montant cible doit être strictement positif.");
        }
    }

    // Anciennement Portefeuille.genererIdObjectif() : lire le compteur puis l'incrémenter était
    // un traitement, pas un attribut ni un getter/setter classique. Portefeuille n'expose plus
    // que getProchainIdObjectif()/setProchainIdObjectif(int) ; c'est ce service qui combine les
    // deux pour distribuer l'identifiant suivant. Même principe que
    // ServiceTransaction.genererIdTransaction().
    private int genererIdObjectif(Portefeuille portefeuille) {
        int id = portefeuille.getProchainIdObjectif();
        portefeuille.setProchainIdObjectif(id + 1);
        return id;
    }

    // Crée un nouvel objectif et l'ajoute au portefeuille. Le compteur d'identifiants reste
    // dans Portefeuille (donc sauvegardé) : repartir de zéro au redémarrage créerait des
    // doublons.
    public Epargne creerObjectif(String nom, double montantCible, LocalDate dateLimite) {
        validerNomObjectif(nom);
        validerMontantCible(montantCible);

        Portefeuille portefeuille = servicePortefeuille.getDonnees();
        Epargne objectif = new Epargne(genererIdObjectif(portefeuille), nom, montantCible, dateLimite);
        portefeuille.ajouterObjectif(objectif);
        servicePortefeuille.sauvegarder();
        return objectif;
    }

    // Anciennement MouvementEpargne.validerMontant(). Appelée à la fois par contribuerObjectif()
    // et retirerObjectif(), qui construisent chacune un MouvementEpargne.
    private void validerMontantMouvement(double montant) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif.");
        }
    }

    // Anciennement MouvementEpargne.validerSens(). Le sens n'est jamais fourni par
    // l'utilisateur (toujours SensMouvement.CONTRIBUTION ou RETRAIT, choisi par
    // contribuerObjectif()/retirerObjectif()) ; ce contrôle reste défensif.
    private void validerSensMouvement(SensMouvement sens) {
        if (sens == null) {
            throw new IllegalArgumentException("Le sens du mouvement est obligatoire.");
        }
    }

    // Anciennement MouvementEpargne.validerDate(). Mêmes deux appelantes que
    // validerMontantMouvement().
    private void validerDateMouvement(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("La date est obligatoire.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date ne peut pas être dans le futur.");
        }
    }

    // Un objectif d'épargne fonctionne comme un coffre : impossible d'y placer une somme
    // dont on ne dispose pas. La vérification se fait ici, et pas dans Epargne, car Epargne
    // ne connaît pas le solde disponible du portefeuille. Refusé à cause de l'état actuel
    // du portefeuille (pas d'une donnée invalide en soi) : IllegalStateException. Les contrôles
    // sur la donnée elle-même (montant, date) passent avant : inutile de vérifier le solde pour
    // un montant qui n'est de toute façon pas valide.
    public void contribuerObjectif(int idObjectif, double montant, LocalDate date) {
        validerMontantMouvement(montant);
        validerSensMouvement(SensMouvement.CONTRIBUTION);
        validerDateMouvement(date);

        double soldeDisponible = servicePortefeuille.getSoldeDisponible();
        if (montant > soldeDisponible) {
            throw new IllegalStateException("Le montant de la contribution dépasse le solde disponible ("
                    + soldeDisponible + " FCFA).");
        }

        Epargne objectif = trouverObjectif(idObjectif);
        objectif.ajouterMouvement(new MouvementEpargne(montant, SensMouvement.CONTRIBUTION, date));
        servicePortefeuille.sauvegarder();
    }

    // Le retrait est refusé si le montant demandé dépasse ce qui est réellement épargné : ce
    // n'est pas le montant en lui-même qui est invalide, c'est l'état actuel de l'objectif qui
    // ne le permet pas (IllegalStateException, pas IllegalArgumentException).
    public void retirerObjectif(int idObjectif, double montant, LocalDate date) {
        validerMontantMouvement(montant);
        validerSensMouvement(SensMouvement.RETRAIT);
        validerDateMouvement(date);

        Epargne objectif = trouverObjectif(idObjectif);
        if (montant > getMontantActuel(objectif)) {
            throw new IllegalStateException("Le montant du retrait ne peut pas dépasser le montant actuellement épargné.");
        }

        objectif.ajouterMouvement(new MouvementEpargne(montant, SensMouvement.RETRAIT, date));
        servicePortefeuille.sauvegarder();
    }

    // Un objectif ne peut être supprimé que s'il est vide : l'utilisateur doit d'abord décider
    // de la destination des sommes qui y étaient placées. Refusé à cause de l'état de l'objectif
    // (pas d'une donnée invalide) : IllegalStateException.
    public void supprimerObjectif(int idObjectif) {
        Portefeuille portefeuille = servicePortefeuille.getDonnees();
        Epargne objectif = trouverObjectif(idObjectif);
        if (!estVide(objectif)) {
            throw new IllegalStateException("L'objectif n'est pas vide (" + getMontantActuel(objectif)
                    + " FCFA restants), retirez d'abord les sommes épargnées.");
        }
        portefeuille.retirerObjectif(objectif);
        servicePortefeuille.sauvegarder();
    }
}
