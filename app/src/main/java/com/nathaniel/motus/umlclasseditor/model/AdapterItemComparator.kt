package com.nathaniel.motus.umlclasseditor.model

class AdapterItemComparator : Comparator<AdapterItem> {
    override fun compare(item1: AdapterItem, item2: AdapterItem): Int {
        val name1 = item1.name ?: ""
        val name2 = item2.name ?: ""
        return name1.compareTo(name2)
    }
}