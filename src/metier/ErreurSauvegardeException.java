package metier;

/*
    * Levée quand l'écriture du fichier de sauvegarde échoue (ex. disque plein, droits
    * d'accès refusés). L'opération métier qui a déclenché la sauvegarde a déjà été
    * appliquée en mémoire à ce moment-là : les données ne sont pas perdues pour la
    * session en cours, seulement pas encore écrites sur le disque. C'est à Menu de
    * décider comment avertir l'utilisateur.
*/
public class ErreurSauvegardeException extends RuntimeException {
    public ErreurSauvegardeException(String message, Throwable cause) {
        super(message, cause);
    }
}
