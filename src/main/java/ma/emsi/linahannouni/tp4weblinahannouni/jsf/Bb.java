package ma.emsi.linahannouni.tp4weblinahannouni.jsf;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import ma.emsi.linahannouni.tp4weblinahannouni.llm.LlmClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class Bb implements Serializable {
    /**
     * Rôle "système" que l'on attribuera plus tard à un LLM.
     * Valeur par défaut que l'utilisateur peut modifier.
     * Possible d'écrire un nouveau rôle dans la liste déroulante.
     */
    private String roleSysteme;








    private boolean roleSystemeChangeable = true;
    public boolean isRoleSystemeChangeable() { return roleSystemeChangeable; }
    public void setRoleSystemeChangeable(boolean b) { this.roleSystemeChangeable = b; }


    private List<SelectItem> listeRolesSysteme;
    @Inject
    private LlmClient llmClient;

    /**
     * Dernière question posée par l'utilisateur.
     */
    private String question;
    /**
     * Dernière réponse de l'API OpenAI.
     */
    private String reponse;
    /**
     * La conversation depuis le début.
     */
    private StringBuilder conversation = new StringBuilder();

    /**
     * Contexte JSF. Utilisé pour qu'un message d'erreur s'affiche dans le formulaire.
     */
    @Inject
    private FacesContext facesContext;

    /**
     * Obligatoire pour un bean CDI (classe gérée par CDI), s'il y a un autre constructeur.
     */
    public Bb() {
    }

    public String getRoleSysteme() {
        return roleSysteme;
    }


    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
        if (llmClient != null && roleSysteme != null && !roleSysteme.isBlank()) {
            llmClient.setSystemRole(roleSysteme);
        }
    }


    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    /**
     * setter indispensable pour le textarea.
     *
     * @param reponse la réponse à la question.
     */
    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(conversation);
    }

    /**
     * Envoie la question au serveur.
     * En attendant de l'envoyer à un LLM, le serveur fait un traitement quelconque, juste pour tester :
     * Le traitement consiste à copier la question en minuscules et à l'entourer avec "||". Le rôle système
     * est ajouté au début de la première réponse.
     *
     * @return null pour rester sur la même page.
     */
    public String envoyer() {
        if (question == null || question.isBlank()) {
            // Erreur ! Le formulaire va être réaffiché en réponse à la requête POST, avec un message d'erreur.
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Texte question vide", "Il manque le texte de la question");
            facesContext.addMessage(null, message);
            return null;
        }

        try {
            if (roleSystemeChangeable) {
                llmClient.setSystemRole(roleSysteme);
                roleSystemeChangeable = false;
            }
            this.reponse = llmClient.envoyerQuestion(question);

        } catch (Exception e) {
            FacesMessage message =
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Problème de connexion avec l'API du LLM",
                            "Problème de connexion avec l'API du LLM" + e.getMessage());
            facesContext.addMessage(null, message);
        }





        afficherConversation();
        return null;


    }

    /**
     * Pour un nouveau chat.
     * Termine la portée view en retournant "index" (la page index.xhtml sera affichée après le traitement
     * effectué pour construire la réponse) et pas null. null aurait indiqué de rester dans la même page (index.xhtml)
     * sans changer de vue.
     * Le fait de changer de vue va faire supprimer l'instance en cours du backing bean par CDI et donc on reprend
     * tout comme au début puisqu'une nouvelle instance du backing va être utilisée par la page index.xhtml.
     * @return "index"
     */
    public String nouveauChat() {
        return "index";
    }

    /**
     * Pour afficher la conversation dans le textArea de la page JSF.
     */
    private void afficherConversation() {
        this.conversation.append("== User:\n").append(question).append("\n== Serveur:\n").append(reponse).append("\n");
    }

    public List<SelectItem> getRolesSysteme() {
        if (this.listeRolesSysteme == null) {
            // Génère les rôles de l'API prédéfinis
            this.listeRolesSysteme = new ArrayList<>();
            // Vous pouvez évidemment écrire ces rôles dans la langue que vous voulez.
            String role = """
                    You are a helpful assistant. You help the user to find the information they need.
                    If the user type a question, you answer it.
                    """;
            // 1er argument : la valeur du rôle, 2ème argument : le libellé du rôle
            this.listeRolesSysteme.add(new SelectItem(role, "Assistant"));

            role = """
                    You are an interpreter. You translate from English to French and from French to English.
                    If the user type a French text, you translate it into English.
                    If the user type an English text, you translate it into French.
                    If the text contains only one to three words, give some examples of usage of these words in English.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Traducteur Anglais-Français"));

            role = """
                    Your are a travel guide. If the user type the name of a country or of a town,
                    you tell them what are the main places to visit in the country or the town
                    are you tell them the average price of a meal.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Guide touristique"));

            role = """
                    You analyse the sentiment of the user it can be positive ,negative or neutral.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Analyseur de sentiments"));

            role = """
        You are a compassionate mental health assistant.
        Your role is to help people who express sadness, hopelessness, or suicidal thoughts.
        You listen carefully, reply with empathy and kindness, and encourage them to talk to someone they trust or a mental health professional.
        You must NEVER give medical advice or judgment.
        Always end your answers with a positive message of hope and remind the user that help is available.
        Use a calm, warm, and reassuring tone.
        """;
            this.listeRolesSysteme.add(new SelectItem(role, "Conseiller bien-être "));
        }

        return this.listeRolesSysteme;
    }





    public String analyserSentiment() {
        if (question == null || question.trim().isEmpty()) {
            reponse = "Veuillez entrer une phrase.";
            return null;
        }

        String lower = question.toLowerCase();
        int score = 0;

        if (lower.contains("bien") || lower.contains("excellent") || lower.contains("bravo"))
            score++;
        if (lower.contains("mal") || lower.contains("mauvais") || lower.contains("triste"))
            score--;

        if (score > 0)
            reponse = " Sentiment positif détecté.";
        else if (score < 0)
            reponse = "Sentiment négatif détecté.";
        else
            reponse = " Sentiment neutre.";

        return null;
    }

}


