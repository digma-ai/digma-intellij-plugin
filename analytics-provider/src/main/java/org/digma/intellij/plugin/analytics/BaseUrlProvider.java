package org.digma.intellij.plugin.analytics;

import javax.annotation.Nonnull;

//provides dynamically changing url
public interface BaseUrlProvider {

    @Nonnull
    String baseUrl();

    void addUrlChangedListener(UrlChangedListener urlChangedListener, int order);

    void removeUrlChangedListener(UrlChangedListener urlChangedListener);


    interface UrlChangedListener {
        void urlChanged(UrlChangedEvent urlChangedEvent);
    }

    class UrlChangedEvent {
        String oldUrl;
        String newUrl;

        public UrlChangedEvent(String oldUrl, String newUrl) {
            this.oldUrl = oldUrl;
            this.newUrl = newUrl;
        }
    }

}


