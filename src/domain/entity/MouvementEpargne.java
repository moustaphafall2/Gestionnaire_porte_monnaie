package domain.entity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import domain.enumeration.SensMouvement;
/*
    * La classe MouvementEpargne représente un mouvement d'épargne dans le système.
    * Chaque mouvement d'épargne a un montant, un sens (contribution ou retrait), et une date.
    * La classe est utilisée pour suivre les mouvements d'épargne effectués par l'utilisateur, permettant ainsi de gérer les contributions et les retraits d'argent dans le système.
    *
    * Elle ne valide plus rien elle-même : la validation (montant strictement positif, sens non
    * nul, date jamais postérieure au jour) est portée par ServiceEpargne. Toute construction d'un
    * MouvementEpargne doit obligatoirement passer par ce service — un appel direct au
    * constructeur ailleurs dans le code contournerait ces règles.
*/

public class MouvementEpargne {
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private double montant;
    private SensMouvement sens;
    private LocalDate date;

    public MouvementEpargne(double montant, SensMouvement sens, LocalDate date)
    {
        this.montant = montant;
        this.sens = sens;
        this.date = date;
    }

    // Getters
    public double getMontant() {
        return montant;
    }
    public SensMouvement getSens() {
        return sens;
    }
    public LocalDate getDate() {
        return date;
    }

    @Override
    public String toString() {
        String signe = (getSens() == SensMouvement.CONTRIBUTION) ? "+" : "-";
        String libelle = (getSens() == SensMouvement.CONTRIBUTION) ? "contribution" : "retrait";
        return "[" + getDate().format(FORMAT_DATE) + "] " + signe + getMontant() + " FCFA (" + libelle + ")";
    }
}