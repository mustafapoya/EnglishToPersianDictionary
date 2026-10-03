package net.golbarg.engtoper.models;

public class SearchHistoryItem {
    private int id;
    private String query;
    private String lang; // "en" or "fa"
    private long timestamp;

    public SearchHistoryItem(int id, String query, String lang, long timestamp) {
        this.id = id;
        this.query = query;
        this.lang = lang;
        this.timestamp = timestamp;
    }

    public SearchHistoryItem(String query, String lang, long timestamp) {
        this.query = query;
        this.lang = lang;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
