package com.denisk.bullshitbingochampion;

import java.util.HashSet;
import java.util.List;

/**
 * Shows which rows and/or columns did hit bingo
 *
 * @author denisk
 * @since 04.01.15.
 */
public class BingoData {
    HashSet<Integer> bingoRows = new HashSet<>();
    HashSet<Integer> bingoColumns = new HashSet<>();

    private BingoData(){}

    public static BingoData fromWords(List<WordAndHits> words) {
        int dim = Util.getDim(words.size());
        BingoData result = new BingoData();
        if(dim < 0) {
            return result;
        }
        for (int i = 0; i < dim; i++) {
            //check i-th row for bingo
            boolean bingo = true;
            for (int j = 0; j < dim; j++) {
                bingo &= words.get(i * dim + j).hits > 0;
            }

            if(bingo) {
                result.bingoRows.add(i);
            }
            bingo = true;

            //check i-th column for bingo
            for (int j = 0; j < dim; j++) {
                bingo &= words.get(j * dim + i).hits > 0;
            }
            if(bingo) {
                result.bingoColumns.add(i);
            }
        }
        return result;
    }
}
