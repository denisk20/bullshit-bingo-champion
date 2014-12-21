package com.denisk.bullshitbingochampion;

/**
 * How many times was the word selected
 */
class WordAndHits {
    String word = "";
    int hits;

    WordAndHits() {
    }

    WordAndHits(String word, int hits) {
        this.word = word;
        this.hits = hits;
    }

    @Override
    public String toString() {
        return "{" +
                "word='" + word + '\'' +
                ", hits=" + hits +
                '}';
    }
}
