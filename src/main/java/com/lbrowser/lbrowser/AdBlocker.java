package com.lbrowser.lbrowser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AdBlocker {
    private static final String URL_ALLOWLIST_GENERAL_HIDE = "https://raw.githubusercontent.com/easylist/easylist/refs/heads/master/easylist/easylist_allowlist_general_hide.txt";
    private static final String URL_GENERAL_HIDE = "https://raw.githubusercontent.com/easylist/easylist/refs/heads/master/easylist/easylist_general_hide.txt";
    private static final String URL_SPECIFIC_HIDE = "https://raw.githubusercontent.com/easylist/easylist/refs/heads/master/easylist/easylist_specific_hide.txt";

    private final Set<String> generalHidingSelectors = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Set<String>> domainSpecificHidingSelectors = Collections.synchronizedMap(new HashMap<>());
    private final Set<String> generalExceptionSelectors = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Set<String>> domainSpecificExceptionSelectors = Collections.synchronizedMap(new HashMap<>());

    private volatile boolean enabled = false;
    private volatile boolean rulesLoaded = false;

    public AdBlocker(){
        loadRulesAsync();
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
        if(enabled && !rulesLoaded){
            loadRulesAsync();
        }
    }

    public boolean isEnabled(){
        return enabled;
    }

    public boolean areRulesLoaded(){
        return rulesLoaded;
    }

    public CompletableFuture<Void> loadRulesAsync(){
        if(rulesLoaded && !generalHidingSelectors.isEmpty()){
            return CompletableFuture.completedFuture(null);
        }

        rulesLoaded = false;

        CompletableFuture<Void> futureAllowList = downloadAndParseList(URL_ALLOWLIST_GENERAL_HIDE, true);
        CompletableFuture<Void> futureGeneral = downloadAndParseList(URL_GENERAL_HIDE, false);
        CompletableFuture<Void> futureSpecific = downloadAndParseList(URL_SPECIFIC_HIDE, false);

        return CompletableFuture.allOf(futureAllowList, futureGeneral, futureSpecific)
                .thenRun(() -> {
                    rulesLoaded = true;
                    //System.out.println("AdBlocker: Rules loaded successfully.");
                    //System.out.println("AdBlocker: General Hide: " + generalHidingSelectors.size());
                    //System.out.println("AdBlocker: Domain Specific Hide: " + domainSpecificHidingSelectors.size());
                    //System.out.println("AdBlocker: General Allow : " + generalExceptionSelectors.size());
                    //System.out.println("AdBlocker: Domain Specific Allow : " + domainSpecificExceptionSelectors.size());
                })
                .exceptionally(ex -> {
                    System.err.println("AdBlocker: Failed to load rules: " + ex.getMessage());
                    rulesLoaded = false;
                    return null;
                });
    }

    private CompletableFuture<Void> downloadAndParseList(String listUrl, boolean isAllowList){
        return CompletableFuture.runAsync(() ->  {
            HttpURLConnection connection = null;
            try{
                URL url = new URL(listUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "LBrowser-AdBlock-Client/1.0"); //be polite
                connection.setConnectTimeout(15000); // 15 seconds
                connection.setReadTimeout(30000); // 30 seconds

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK){
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))){
                        reader.lines().forEach(line -> parseLine(line, isAllowList));
                    }
                }else{
                    System.err.println("Failed to download list: " + listUrl + ": " + connection.getResponseCode());
                }

            }catch(Exception e){
                System.err.println("AdBlocker: Error processing list: "+ listUrl + ": " +  e.getMessage());
            }finally{
                if(connection != null){
                    connection.disconnect();
                }
            }
        });
    }

    private void parseLine(String line, boolean isAllowList){
        line = line.trim();

        if(line.isEmpty() || line.startsWith("!") || line.startsWith("[") || line.startsWith("@")){
            return;
        }

        String separator = isAllowList ? "#@##" : "##";
        int separatorPos = line.indexOf(separator);

        if(separatorPos == -1){
            return;
        }

        String domainPart = line.substring(0, separatorPos);
        String selectorPart = line.substring(separatorPos + separator.length());
        //selectorPart = selectorPart.replaceAll("\\{.*\\}", "").trim();
        //selectorPart = selectorPart.replaceAll("^([a-zA-Z0-9*_-]+)~", "$1");

        if(selectorPart.isEmpty()){
            return;
        }

        Set<String> targetGeneralSet = isAllowList ? generalExceptionSelectors : generalHidingSelectors;
        Map<String, Set<String>> targetDomainMap = isAllowList ? domainSpecificExceptionSelectors : domainSpecificHidingSelectors;
        Set<String> targetDomainSet = targetDomainMap.computeIfAbsent(domainPart, k -> Collections.synchronizedSet(new HashSet<>()));

        if(domainPart.isEmpty()){
            targetGeneralSet.add(selectorPart);
        }else{
            if (domainPart.startsWith("~")){
                return;
            }
            String[] domains = domainPart.split(",");
            for (String domain : domains){
                domain = domain.trim().toLowerCase();
                if(!domain.isEmpty()){
                    targetDomainMap.computeIfAbsent(domain, k -> Collections.synchronizedSet(new HashSet<>())).add(selectorPart);
                }
            }
        }
    }

    public String getEffectiveCssForDomain(String urlString){
        if (!enabled || !rulesLoaded || urlString == null || urlString.isEmpty() || urlString.startsWith("data:") || urlString.startsWith("about:")) {
            return "";
        }
        String host = null;
        try{
            URL url = new URL(urlString);
            host = url.getHost().toLowerCase();
            if(host == null || host.isEmpty() || host.equals("localhost")){
                return "";
            }
        }catch(MalformedURLException e){
            return "";
        }

        Set<String> effectiveSelectors = new HashSet<>();

        effectiveSelectors.addAll(generalHidingSelectors);
        addSelectorsForDomain(domainSpecificHidingSelectors, host, effectiveSelectors);
        String baseDomain = getBaseDomain(host);
        if (baseDomain != null && !baseDomain.equals(host)){
            addSelectorsForDomain(domainSpecificHidingSelectors, baseDomain, effectiveSelectors);
        }

        effectiveSelectors.removeAll(generalExceptionSelectors);

        removeSelectorsForDomain(domainSpecificExceptionSelectors, host, effectiveSelectors);
        if (baseDomain != null && !baseDomain.equals(host)) {
            removeSelectorsForDomain(domainSpecificExceptionSelectors, baseDomain, effectiveSelectors);
        }

        if (effectiveSelectors.isEmpty()) {
            return "";
        }

        return effectiveSelectors.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(",\n"))
                + "\n{ display: none !important; visibility: hidden !important; }";
    }

    private void addSelectorsForDomain(Map<String, Set<String>> domainMap, String domain, Set<String> targetSet) {
        Set<String> selectors = domainMap.get(domain);
        if(selectors != null){
            targetSet.addAll(selectors);
        }
    }

    private void removeSelectorsForDomain(Map<String, Set<String>> domainMap, String domain, Set<String> targetSet){
        Set<String> selectors = domainMap.get(domain);
        if(selectors != null){
            targetSet.removeAll(selectors);
        }
    }

    private String getBaseDomain(String host){
        if(host == null) return null;
        String[] parts = host.split("\\.");
        if (parts.length > 2){
            int last = parts.length - 1;
            int secondLast = parts.length - 2;
            if ( (parts[secondLast] + "." + parts[last]).matches("^(com|net|org|gov|edu|co|io|me)\\.[a-z]{2}$")) {
                if(parts.length > 3){
                    return parts[parts.length - 3];
                }
            }
            return parts[secondLast] + "." + parts[last];
        }
        return host;
    }

    public String createDataUrlForCss(String cssContent){
        if(cssContent == null || cssContent.isEmpty()){
            return null;
        }
        try{
            String encodeCss = URLEncoder.encode(cssContent, StandardCharsets.UTF_8).replace("+", "%20");
            return "data:text/css;charset=utf-8," + encodeCss;
        }catch (Exception e){
            System.err.println("AdBlocker: Error creating data URL for CSS: " + e.getMessage());
            return null;
        }
    }
}
