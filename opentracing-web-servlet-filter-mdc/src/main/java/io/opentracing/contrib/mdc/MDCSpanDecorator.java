package io.opentracing.contrib.mdc;

import io.opentracing.Span;
import io.opentracing.tag.StringTag;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.MDC;

public class MDCSpanDecorator {

    private final String baseTagKey;
    private final List<MDCEntry> mdcEntries;

    public MDCSpanDecorator(List<MDCEntry> mdcEntry) {
        this(mdcEntry, "mdc");
    }

    public MDCSpanDecorator(List<MDCEntry> mdcEntry, String baseTagKey) {
        this.mdcEntries = new ArrayList<>(mdcEntry);
        this.baseTagKey = (baseTagKey != null && !baseTagKey.isEmpty()) ? baseTagKey + "." : null;
    }

    public void decorate(Span span) {
        for (MDCEntry mdcEntry : mdcEntries) {
            String mdcValue = MDC.get(mdcEntry.getKey());
            if (mdcValue != null && !mdcValue.isEmpty()) {
                buildTag(mdcEntry.getTag()).set(span, mdcValue);
            }
        }
    }

    private StringTag buildTag(String tag) {
        if (baseTagKey == null) {
            return new StringTag(tag);
        }
        return new StringTag(baseTagKey + tag);
    }

    protected String getBaseTagKey() {
        return this.baseTagKey;
    }

    protected List<MDCEntry> getMdcEntries() {
        return this.mdcEntries;
    }

    public static class MDCEntry {
        private String key;
        private String tag;

        public MDCEntry(String key, String tag) {
            this.key = key;
            this.tag = tag;
        }

        public MDCEntry() {
        }

        public String getKey() {
            return this.key;
        }

        public String getTag() {
            return this.tag;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }
    }

}
