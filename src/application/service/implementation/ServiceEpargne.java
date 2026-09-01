package application.service.implementation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import application.dto.MouvementDTO;
import application.dto.ObjectifDTO;
import domain.entity.Epargne;
import domain.entity.MouvementEpargne;
import domain.enumeration.SensMouvement;
import application.service.interfaces.IServiceEpargne;
import application.service.interfaces.IServiceSolde;

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
    *
    * Depuis l'étape DTO, il ne renvoie plus jamais d'Epargne ni de MouvementEpargne à la
    * présentation : chaque méthode consultée par ControleurEpargne renvoie un ObjectifDTO ou un
    * MouvementDTO, construit par versAffichage() juste avant de sortir du service.
    *
    * Depuis l'étape 5, le solde disponible (nécessaire à contribuerObjectif()) ne vient plus de
    * ServicePortefeuille mais de ServiceSolde, déclaré par son interface IServiceSolde : c'est
    * le seul de ses deux besoins qui peut passer par une interface, l'autre (détenir/sauvegarder
    * via getDonnees()) reste forcé de dépendre de la classe concrète ServicePortefeuille (voir
    * le journal de développement pour la raison technique).
    *
    * Depuis l'étape 6, l'identifiant d'un nouvel objectif vient de la base
    * (servicePortefeuille.enregistrerNouvelObjectif()) : plus de compteur à combiner soi-même.
    * Ordre systématique désormais : persister d'abord, construire ou muter la mémoire ensuite.
*/
public class ServiceEpargne implements IServiceEpargne {
    // Les montants sont des FCFA sans centimes, mais restent des double : deux montants
    // "égaux" en théorie peuvent différer d'une poussière après une suite d'additions/
    // soustractions. On compare donc à un epsilon près plutôt qu'avec ==.
    private static final double EPSILON = 0.01;

    private final ServicePortefeuille servicePortefeuille;
    private final IServiceSolde serviceSolde;

    public ServiceEpargne(ServicePortefeuille servicePortefeuille, IServiceSolde serviceSolde) {
        this.servicePortefeuille = servicePortefeuille;
        this.serviceSolde = serviceSolde;
    }

    // Montant actuellement épargné sur cet objectif. Le calcul passe par CalculEpargne, partagé
    // avec ServicePortefeuille (getTotalEpargne), pour qu'il n'existe qu'à un seul endroit.
    // Privée : depuis l'étape DTO, plus aucun appelant hors de ce service n'a besoin du montant
    // actuel isolé, il arrive toujours déjà inclus dans un ObjectifDTO.
    private double getMontantActuel(Epargne objectif) {
        return CalculEpargne.calculerMontantActuel(objectif);
    }

    private double getPourcentageAtteint(Epargne objectif) {
        return (getMontantActuel(objectif) / objectif.getMontantCible()) * 100;
    }

    // Indique si une contribution de ce montant ferait dépasser le montant cible. Dépasser la
    // cible reste autorisé (règle de gestion) : c'est à l'appelant (ControleurEpargne) de
    // décider s'il signale ce dépassement avant de confirmer l'opération. Prend un idObjectif et
    // non un Epargne : ControleurEpargne ne détient plus d'entité, seulement l'id saisi par
    // l'utilisateur et l'ObjectifDTO reçu de getObjectif().
    public boolean depasseraCible(int idObjectif, double montant) {
        Epargne objectif = trouverObjectif(idObjectif);
        return getMontantActuel(objectif) + montant > objectif.getMontantCible();
    }

    // Condition nécessaire à la suppression de l'objectif
    private boolean estVide(Epargne objectif) {
        return Math.abs(getMontantActuel(objectif)) < EPSILON;
    }

    // Liste complète des objectifs, prête à afficher : chaque ObjectifDTO porte déjà son montant
    // actuel et son pourcentage atteint, calculés ici une fois pour toute la liste. Avant l'étape
    // DTO, ControleurEpargne recevait la liste d'Epargne et deux listes parallèles de Double ;
    // les trois sont fusionnées dans le DTO, un par objectif.
    public List<ObjectifDTO> getObjectifs() {
        List<ObjectifDTO> resultat = new ArrayList<>();
        for (Epargne objectif : servicePortefeuille.getDonnees().getObjectifs()) {
            resultat.add(versAffichage(objectif));
        }
        return resultat;
    }

    // Recherche publique d'un objectif par id, utilisée par ControleurEpargne pour récupérer le
    // nom de l'objectif choisi avant de contribuer, retirer ou en afficher le détail. Renvoie le
    // DTO, jamais l'entité elle-même.
    public ObjectifDTO getObjectif(int idObjectif) {
        return versAffichage(trouverObjectif(idObjectif));
    }

    // Mouvements (contributions et retraits) d'un objectif, pour l'écran détail. Anciennement
    // objectif.getMouvements() appelé directement par ControleurEpargne sur l'entité reçue de
    // getObjectif() ; depuis l'étape DTO, ControleurEpargne ne détient plus d'Epargne, donc plus
    // aucun moyen d'atteindre ses mouvements sans passer par une méthode du service.
    public List<MouvementDTO> getMouvements(int idObjectif) {
        List<MouvementDTO> resultat = new ArrayList<>();
        for (MouvementEpargne mouvement : trouverObjectif(idObjectif).getMouvements()) {
            resultat.add(versAffichage(mouvement));
        }
        return resultat;
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

    // Construit le DTO transmis à la présentation à partir d'une Epargne du domaine : une copie
    // de ses champs, plus montantActuel et pourcentageAtteint déjà calculés (le DTO ne doit
    // jamais recalculer quoi que ce soit lui-même).
    private ObjectifDTO versAffichage(Epargne objectif) {
        return new ObjectifDTO(objectif.getId(), objectif.getNom(), objectif.getMontantCible(),
                getMontantActuel(objectif), getPourcentageAtteint(objectif));
    }

    // Même principe que versAffichage(Epargne), pour un mouvement.
    private MouvementDTO versAffichage(MouvementEpargne mouvement) {
        return new MouvementDTO(mouvement.getMontant(), mouvement.getSens(), mouvement.getDate());
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

    // Crée un nouvel objectif et l'ajoute au portefeuille. Renvoie void : comme
    // ajouterDepense/ajouterRevenu, la valeur créée n'était jamais lue par ControleurEpargne, la
    // garder aurait été du code mort.
    public void creerObjectif(String nom, double montantCible, LocalDate dateLimite) {
        validerNomObjectif(nom);
        validerMontantCible(montantCible);

        int id = servicePortefeuille.enregistrerNouvelObjectif(nom, montantCible, dateLimite);
        Epargne objectif = new Epargne(id, nom, montantCible, dateLimite);
        servicePortefeuille.getDonnees().ajouterObjectif(objectif);
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

        double soldeDisponible = serviceSolde.getSoldeDisponible();
        if (montant > soldeDisponible) {
            throw new IllegalStateException("Le montant de la contribution dépasse le solde disponible ("
                    + soldeDisponible + " FCFA).");
        }

        Epargne objectif = trouverObjectif(idObjectif);
        servicePortefeuille.enregistrerNouveauMouvement(idObjectif, montant, SensMouvement.CONTRIBUTION, date);
        objectif.ajouterMouvement(new MouvementEpargne(montant, SensMouvement.CONTRIBUTION, date));
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

        servicePortefeuille.enregistrerNouveauMouvement(idObjectif, montant, SensMouvement.RETRAIT, date);
        objectif.ajouterMouvement(new MouvementEpargne(montant, SensMouvement.RETRAIT, date));
    }

    // Un objectif ne peut être supprimé que s'il est vide : l'utilisateur doit d'abord décider
    // de la destination des sommes qui y étaient placées. Refusé à cause de l'état de l'objectif
    // (pas d'une donnée invalide) : IllegalStateException.
    public void supprimerObjectif(int idObjectif) {
        Epargne objectif = trouverObjectif(idObjectif);
        if (!estVide(objectif)) {
            throw new IllegalStateException("L'objectif n'est pas vide (" + getMontantActuel(objectif)
                    + " FCFA restants), retirez d'abord les sommes épargnées.");
        }
        servicePortefeuille.enregistrerSuppressionObjectif(idObjectif);
        servicePortefeuille.getDonnees().retirerObjectif(objectif);
    }
}
