package view;

public final class NpcDialogLine {
    private final String speakerName;
    private final String portraitPath;
    private final String text;

    public NpcDialogLine(String speakerName, String portraitPath, String text) {
        this.speakerName = speakerName;
        this.portraitPath = portraitPath;
        this.text = text;
    }

    public String getSpeakerName() {
        return this.speakerName;
    }

    public String getPortraitPath() {
        return this.portraitPath;
    }

    public String getText() {
        return this.text;
    }
}
