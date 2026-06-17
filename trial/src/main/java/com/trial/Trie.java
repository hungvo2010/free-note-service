package com.trial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class Trie {
    private static final int DEFAULT_TRIE_SIZE = 10000;
    private static final int TOTAL_CHARS = 26;
    private int size = 0;
    private final List<Node> nodes = new ArrayList<>(DEFAULT_TRIE_SIZE);

    public Trie() {
        nodes.add(new Node());
    }

    public void insert(String word) {
        int pos = 0;
        for (int i = 0; i < word.length(); i++) {
            int index = word.charAt(i) - 'a';
            if (nodes.get(pos).children[index] == -1) {
                var newNode = createNewNode();
                nodes.add(newNode);
                nodes.get(pos).children[index] = size;
                pos = size;
//                continue;
            } else {
                var nodePos = nodes.get(pos).children[index];
//            pos = nodes.get(pos).children[index];
                nodes.get(nodePos).count++;
                pos = nodePos;
            }

            if (i == word.length() - 1) {
                nodes.get(pos).exist++;
            }
        }
    }

    private Node createNewNode() {
        var newNode = new Node();
        newNode.count = 1;
        size++;
        return newNode;
    }

    public boolean search(String word) {
        int pos = 0;
//        int lastPos = 0;
        for (int i = 0; i < word.length(); i++) {
            int index = word.charAt(i) - 'a';
            if (nodes.get(pos).children[index] == -1) {
                return false;
            }
//            lastPos = pos;
            pos = nodes.get(pos).children[index];
        }
        return nodes.get(pos).exist != 0;
    }

    public boolean startsWith(String prefix) {
        int pos = 0;
        for (int i = 0; i < prefix.length(); i++) {
            int index = prefix.charAt(i) - 'a';
            if (nodes.get(pos).children[index] == -1) {
                return false;
            }
            pos = nodes.get(pos).children[index];
        }
        return true;
    }

    public static class Node {
        int exist;
        int count;
        int[] children;

        public Node() {
            children = new int[26];
            count = 0;
            exist = 0;
            Arrays.fill(children, -1);
        }
    }

    public static void main(String[] args) {
        Trie trie = new Trie();
        trie.insert("apple");
        System.out.println("search apple: " + trie.search("apple"));
        System.out.println("suggested products: " + suggestedProducts(new String[]{"mobile", "mouse", "moneypot", "monitor", "mousepad"}, "mouse"));
    }

    public static List<List<String>> suggestedProducts(String[] products, String searchWord) {
        Trie trie = new Trie();
        for (String product : products) {
            trie.insert(product);
        }
        List<List<String>> results = new ArrayList<>();
        for (int i = 0; i < searchWord.length(); i++) {
            var c = searchWord.substring(0, i + 1);
            List<String> suggestions = trie.searchStringsWithPrefix(c);
            int k = 3;
            results.add(getTopKResults(suggestions, k));
        }
        return results;
    }

    private static List<String> getTopKResults(List<String> suggestions, int topK) {
        return suggestions.stream().sorted().limit(topK).collect(Collectors.toList());
    }

    private List<String> searchStringsWithPrefix(String prefix) {
        int pos = 0;
        for (int i = 0; i < prefix.length(); i++) {
            int index = prefix.charAt(i) - 'a';
            if (nodes.get(pos).children[index] == -1) {
                return Collections.emptyList();
            }
            pos = nodes.get(pos).children[index];
        }
        List<String> nodeDiscover = discoverNode(pos);
        return mergePrefix(prefix, nodeDiscover);
    }

    private List<String> mergePrefix(String prefix, List<String> arr) {
        return arr.stream()
                .map(s -> prefix + s)
                .collect(Collectors.toList());
    }

    private List<String> discoverNode(int pos) {
        var results = new ArrayList<String>();
        if (nodes.get(pos).exist > 0) {
            results.add("");
        }
        for (int i = 0; i < TOTAL_CHARS; ++i) {
            if (nodes.get(pos).children[i] != -1) {
                String currentChar = String.valueOf((char) (i + 'a'));
                List<String> childWords = discoverNode(nodes.get(pos).children[i]);
                results.addAll(mergePrefix(currentChar, childWords));
            }
        }
        return results;
    }
}
