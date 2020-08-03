package com.bulletjournal.util;

import com.bulletjournal.exceptions.BadRequestException;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;

import java.util.*;

public class DeltaConverter {

    private static Gson GSON = new Gson();

    public static String supplementContentText(String text) {
        // from web: {delta: YYYYY2, ###html###:ZZZZZZ2}
        // from mobile: {mdelta:XXXXXX }
        DeltaContent deltaContent = new DeltaContent(text);

        if (deltaContent.hasDeltaMap() && deltaContent.hasMdeltaList()) {
            throw new BadRequestException("Cannot have both delta and mdelta");
        }
        if (deltaContent.hasDeltaMap()) {
            deltaContent.setMdeltaList(deltaTomDelta(deltaContent.getDeltaMap()));
        } else if (deltaContent.hasMdeltaList()) {
            deltaContent.setDeltaMap(mDeltaToDelta(deltaContent.getMdeltaList()));
        } else {
            throw new BadRequestException("None of delta and mdelta exists");
        }

        return deltaContent.toJSON();
    }

    public static String generateDeltaContent(String contentStr) {
        String content = "{\"delta\":{\"ops\":[{\"insert\":\"TEMPLATE\\n\"}]},\"###html###\":\"<p>TEMPLATE</p><p><br></p>\"}";
        return content.replace("TEMPLATE", contentStr);
    }

    public static List<LinkedHashMap> diffToMdiff(final Map diffMap) {
        return deltaTomDelta(diffMap);
    }

    public static void main(String[] args) {
        String input = "{\"ops\":[{\"retain\":5,\"attributes\":{\"bold\":true}}]}";
        Map diffMap = GSON.fromJson(input, LinkedHashMap.class);
        List res = diffToMdiff(diffMap);
        System.out.println(res);
    }

    @VisibleForTesting
    protected static String mDeltaToDeltaStr(final String mDelta) {
        List<Map<String, Object>> mDeltaList = GSON.fromJson(mDelta, List.class);
        LinkedHashMap opsMap = mDeltaToDelta(mDeltaList);
        LinkedHashMap deltaMap = new LinkedHashMap();
        deltaMap.put("delta", opsMap);
        return GSON.toJson(deltaMap);
    }

    protected static LinkedHashMap mDeltaToDelta(final List<Map<String, Object>> mDeltaList) {
        List<LinkedHashMap<String, Object>> deltaList = new ArrayList<>();
        for (Map<String, Object> eDelta : mDeltaList) {
            LinkedHashMap<String, Object> clonedMap = new LinkedHashMap<>();
            // each attributes and insert
            for (Map.Entry<String, Object> e : eDelta.entrySet()) {
                if (Objects.equals(e.getKey(), "attributes")) {
                    Map<String, Object> formatMap = (Map<String, Object>) e.getValue();
                    LinkedHashMap clonedAttri = new LinkedHashMap();
                    // bold
                    if (Objects.nonNull(formatMap.get("b"))) {
                        Boolean val = (Boolean) formatMap.get("b");
                        clonedAttri.put("bold", val);
                    }
                    //italic
                    if (Objects.nonNull(formatMap.get("i"))) {
                        Boolean val = (Boolean) formatMap.get("i");
                        clonedAttri.put("italic", val);
                    }

                    //link
                    if (Objects.nonNull(formatMap.get("a"))) {
                        String val = (String) formatMap.get("a");
                        clonedAttri.put("link", val);
                    }
                    //heading
                    if (Objects.nonNull(formatMap.get("heading"))) {
                        Double val = (Double) formatMap.get("heading");
                        Integer intVal = val.intValue();
                        clonedAttri.put("header", intVal);
                    }

                    //block
                    //list
                    if (Objects.nonNull(formatMap.get("block"))) {
                        String val = (String) formatMap.get("block");
                        // order list
                        if (val.equals("ol")) {
                            clonedAttri.put("list", "ordered");
                        }
                        // bullet list
                        if (val.equals("ul")) {
                            clonedAttri.put("list", "bullet");
                        }
                        //code
                        if (val.equals("code")) {
                            clonedAttri.put("code-block", true);
                        }
                        //quote
                        if (val.equals("quote")) {
                            clonedAttri.put("blockquote", true);
                        }
                    }

                    // convert embed : imagine


                    clonedMap.put("attributes", clonedAttri);
                } else {
                    clonedMap.put(e.getKey(), e.getValue());
                }

            }
            deltaList.add(clonedMap);
        }

        LinkedHashMap opsMap = new LinkedHashMap();
        opsMap.put("ops", deltaList);


        return opsMap;
    }

    @VisibleForTesting
    protected static String deltaTomDeltaStr(final String delta) {
        LinkedHashMap<String, Object> map = GSON.fromJson(delta, LinkedHashMap.class);
        Map<String, Object> deltaMap = (Map) map.get("delta");
        return GSON.toJson(deltaTomDelta(deltaMap));
    }

    protected static List<LinkedHashMap> deltaTomDelta(final Map<String, Object> deltaMap) {

        List<LinkedHashMap> opsList = (ArrayList) (deltaMap.get("ops"));
        List<LinkedHashMap> mDeltaList = new ArrayList<>();

        for (Map<String, Object> innerDeltaMap : opsList) {
            LinkedHashMap clonedMap = new LinkedHashMap();
            for (Map.Entry<String, Object> e : innerDeltaMap.entrySet()) {
                if (Objects.equals(e.getKey(), "attributes")) {
                    Map<String, Object> formatMap = (Map<String, Object>) e.getValue();
                    LinkedHashMap clonedAttri = new LinkedHashMap();

                    // bold
                    if (Objects.nonNull(formatMap.get("bold"))) {
                        Boolean val = (Boolean) formatMap.get("bold");
                        clonedAttri.put("b", val);
                    }
                    //italic
                    if (Objects.nonNull(formatMap.get("italic"))) {
                        Boolean val = (Boolean) formatMap.get("italic");
                        clonedAttri.put("i", val);
                    }
                    //link
                    if (Objects.nonNull(formatMap.get("link"))) {
                        String val = (String) formatMap.get("link");
                        clonedAttri.put("a", val);
                    }
                    //heading
                    if (Objects.nonNull(formatMap.get("header"))) {
                        Double val = (Double) formatMap.get("header");
                        Integer intVal = val.intValue();
                        clonedAttri.put("heading", intVal);
                    }
                    //block
                    //list
                    if (Objects.nonNull(formatMap.get("list"))) {
                        String val = (String) formatMap.get("list");
                        // order list
                        if (val.equals("ordered")) {
                            clonedAttri.put("block", "ol");
                        }
                        // bullet list
                        if (val.equals("bullet")) {
                            clonedAttri.put("block", "ul");
                        }
                    }
                    //code
                    if (Objects.nonNull(formatMap.get("code-block"))) {
                        Boolean val = (Boolean) formatMap.get("code-block");
                        if (val) {
                            clonedAttri.put("block", "code");
                        }
                    }
                    //quote
                    if (Objects.nonNull(formatMap.get("blockquote"))) {
                        Boolean val = (Boolean) formatMap.get("blockquote");
                        clonedAttri.put("block", "quote");
                    }

                    // convert embed : imagine

                    clonedMap.put("attributes", clonedAttri);

                } else {
                    clonedMap.put(e.getKey(), e.getValue());
                }
            }

            mDeltaList.add(clonedMap);
        }
        return mDeltaList;
    }
}

