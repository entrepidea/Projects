package com.datonicgroup.narrate.app.models.comparators;

import com.datonicgroup.narrate.app.models.Entry;

import java.util.Comparator;

/**
 * Created by timothymiko on 6/20/14.
 */
public class EntryNewestDateComparator implements Comparator<Entry> {
    @Override
    public int compare(Entry lhs, Entry rhs) {
        return rhs.creationDate.compareTo(lhs.creationDate);
    }
}
