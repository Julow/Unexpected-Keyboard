package juloo.keyboard2;

public class Snippet extends SnippetItem {
    public String content;

    public Snippet(String content) {
        super(content); // Default name is content (truncated likely in UI)
        this.content = content;
    }

    public Snippet(String uuid, String name, String content) {
        super(uuid, name);
        this.content = content;
    }
}
