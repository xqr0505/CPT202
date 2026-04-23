package edu.xjtlu.cpt202.backend.modules.ai.model;

/**
 * RAGAS export item for evaluation.
 */
public class RagResultItem {

    private String question;
    private String answer;
    private String context;
    private String groundTruth;

    public RagResultItem() {
    }

    public RagResultItem(String question, String answer, String context, String groundTruth) {
        this.question = question;
        this.answer = answer;
        this.context = context;
        this.groundTruth = groundTruth;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getGroundTruth() {
        return groundTruth;
    }

    public void setGroundTruth(String groundTruth) {
        this.groundTruth = groundTruth;
    }
}
