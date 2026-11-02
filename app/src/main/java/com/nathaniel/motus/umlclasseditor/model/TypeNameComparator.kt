package com.nathaniel.motus.umlclasseditor.model

class TypeNameComparator : Comparator<String> {
    override fun compare(s1: String, s2: String): Int {
        if (s1.isEmpty() || s2.isEmpty()) return s1.compareTo(s2)

        if (s1[0].isLowerCase() && s2[0].isUpperCase()) return -1
        if (s1[0].isUpperCase() && s2[0].isLowerCase()) return 1

        return s1.compareTo(s2)
    }
}