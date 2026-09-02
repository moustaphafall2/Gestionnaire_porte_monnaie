package application.service.implementation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import application.dto.MouvementDTO;
import application.dto.ObjectifDTO;
import application.mapper.MouvementMapper;
import application.mapper.ObjectifMapper;
import domain.entity.Epargne;
import domain.entity.MouvementEpargne;
import domain.enumeration.SensMouvement;
import application.service.interfaces.IServiceEpargne;
import application.service.interfaces.IServiceSolde;
import infrastructure.persistence.EpargneRepository;

/*
    * ServiceEpargne porte les règles de gestion des objectifs d'épargne : création, contribution,
    * retrait, suppression, et les calculs de progression. Ne renvoie jamais Epargne ni
    * MouvementEpargne à la présentation, seulement des DTO construits par ObjectifMapper et
    * MouvementMapper.
*/
public class ServiceEpargne implements IServiceEpargne {
    // Les montants sont des FCFA sans centimes, mais restent des double : deux montants
    // "égaux" en théorie peuvent différer d'une poussière après une suite d'additions/
    // soustractions. On compare donc à un epsilon près plutôt qu'avec ==.
    private static final double EPSILON = 0.01;

    private final ServicePortefeuille servicePortefeuille;
    private final IServiceSolde serviceSolde;
    private final EpargneRepository epargneRepository;

    public ServiceEpargne(ServicePortefeuille servicePortefeuille, IServiceSolde serviceSolde,
            EpargneRepository epargneRepository) {
        this.servicePortefeuille = servicePortefeuille;
        this.serviceSolde = serviceSolde;
        this.epargneRepository = epargneRepository;
    }

    private double getMontantActuel(Epargne objectif) {
        return CalculEpargne.calculerMontantActuel(objectif);
    }

    private double getPourcentageAtteint(Epargne objectif) {
        return (getMontantActuel(objectif) / objectif.getMontantCible()) * 100;
    }

    public boolean depasseraCible(int idObjectif, double montant) {
        Epargne objectif = trouverObjectif(idObjectif);
        return getMontantActuel(objectif) + montant > objectif.getMontantCible();
    }

    private boolean estVide(Epargne objectif) {
        return Math.abs(getMontantActuel(objectif)) < EPSILON;
    }

    public List<ObjectifDTO> getObjectifs() {
        List<ObjectifDTO> resultat = new ArrayList<>();
        for (Epargne objectif : servicePortefeuille.getDonnees().getObjectifs()) {
            resultat.add(ObjectifMapper.versDTO(objectif, getMontantActuel(objectif), getPourcentageAtteint(objectif)));
        }
        return resultat;
    }

    public ObjectifDTO getObjectif(int idObjectif) {
        Epargne objectif = trouverObjectif(idObjectif);
        return ObjectifMapper.versDTO(objectif, getMontantActuel(objectif), getPourcentageAtteint(objectif));
    }

    public List<MouvementDTO> getMouvements(int idObjectif) {
        List<MouvementDTO> resultat = new ArrayList<>();
        for (MouvementEpargne mouvement : trouverObjectif(idObjectif).getMouvements()) {
            resultat.add(MouvementMapper.versDTO(mouvement));
        }
        return resultat;
    }

    private Epargne trouverObjectif(int idObjectif) {
        for (Epargne objectif : servicePortefeuille.getDonnees().getObjectifs()) {
            if (objectif.getId() == idObjectif) {
                return objectif;
            }
        }
        throw new IllegalArgumentException("Aucun objectif avec l'identifiant " + idObjectif + ".");
    }

    private void validerNomObjectif(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom de l'objectif est obligatoire.");
        }
    }

    private void validerMontantCible(double montantCible) {
        if (montantCible <= 0) {
            throw new IllegalArgumentException("Le montant cible doit être strictement positif.");
        }
    }

    public void creerObjectif(String nom, double montantCible, LocalDate dateLimite) {
        validerNomObjectif(nom);
        validerMontantCible(montantCible);

        int id = epargneRepository.ajouter(nom, montantCible, dateLimite);
        Epargne objectif = new Epargne(id, nom, montantCible, dateLimite);
        servicePortefeuille.getDonnees().ajouterObjectif(objectif);
    }

    private void validerMontantMouvement(double montant) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être strictement positif.");
        }
    }

    private void validerSensMouvement(SensMouvement sens) {
        if (sens == null) {
            throw new IllegalArgumentException("Le sens du mouvement est obligatoire.");
        }
    }

    private void validerDateMouvement(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("La date est obligatoire.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date ne peut pas être dans le futur.");
        }
    }

    // Un objectif d'épargne fonctionne comme un coffre : refusée si le montant dépasse le solde
    // disponible.
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
        epargneRepository.ajouterMouvement(idObjectif, montant, SensMouvement.CONTRIBUTION, date);
        objectif.ajouterMouvement(new MouvementEpargne(montant, SensMouvement.CONTRIBUTION, date));
    }

    // Refusé si le montant dépasse ce qui est réellement épargné.
    public void retirerObjectif(int idObjectif, double montant, LocalDate date) {
        validerMontantMouvement(montant);
        validerSensMouvement(SensMouvement.RETRAIT);
        validerDateMouvement(date);

        Epargne objectif = trouverObjectif(idObjectif);
        if (montant > getMontantActuel(objectif)) {
            throw new IllegalStateException("Le montant du retrait ne peut pas dépasser le montant actuellement épargné.");
        }

        epargneRepository.ajouterMouvement(idObjectif, montant, SensMouvement.RETRAIT, date);
        objectif.ajouterMouvement(new MouvementEpargne(montant, SensMouvement.RETRAIT, date));
    }

    // Suppression autorisée seulement si l'objectif est vide.
    public void supprimerObjectif(int idObjectif) {
        Epargne objectif = trouverObjectif(idObjectif);
        if (!estVide(objectif)) {
            throw new IllegalStateException("L'objectif n'est pas vide (" + getMontantActuel(objectif)
                    + " FCFA restants), retirez d'abord les sommes épargnées.");
        }
        epargneRepository.supprimer(idObjectif);
        servicePortefeuille.getDonnees().retirerObjectif(objectif);
    }
}
