package persistance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;

import metier.ErreurSauvegardeException;
import metier.Portefeuille;

/*
    * La classe GestionnaireFichier est la seule classe autorisée à lire ou écrire sur le disque.
    * Elle isole toute la logique de sérialisation/désérialisation JSON (via Gson) du reste de
    * l'application : Portefeuille n'a jamais besoin de savoir comment les données sont stockées.
    *
    * Gson ne sait pas convertir un LocalDate en JSON par défaut (ce n'est pas un type qu'il
    * reconnaît nativement), il faut donc lui apprendre à le faire avec un adaptateur, enregistré
    * dans le constructeur.
*/
public class GestionnaireFichier {

    private String cheminFichier;
    private Gson gson;

    public GestionnaireFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;

        // Adaptateur pour LocalDate : indique à Gson comment le transformer en texte ("2026-08-10")
        // et comment le relire depuis ce même texte, dans les deux sens.
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (date, type, contexte) ->
                        new JsonPrimitive(date.toString()))
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, contexte) ->
                        LocalDate.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

    // Sérialise le portefeuille en JSON et l'écrit dans le fichier de sauvegarde.
    public void sauvegarder(Portefeuille portefeuille) {
        try (FileWriter fichier = new FileWriter(cheminFichier)) {
            gson.toJson(portefeuille, fichier);
        } catch (IOException exception) {
            throw new ErreurSauvegardeException("Impossible d'écrire le fichier de sauvegarde : " + cheminFichier, exception);
        }
    }

    // Lit le fichier JSON et le désérialise en Portefeuille.
    // Si le fichier n'existe pas encore (premier lancement), retourne un portefeuille vide.
    public Portefeuille charger() {
        if (!fichierExiste()) {
            return new Portefeuille();
        }

        try (FileReader fichier = new FileReader(cheminFichier)) {
            return gson.fromJson(fichier, Portefeuille.class);
        } catch (IOException exception) {
            throw new RuntimeException("Impossible de lire le fichier de sauvegarde : " + cheminFichier, exception);
        }
    }

    public boolean fichierExiste() {
        return new File(cheminFichier).exists();
    }
}
