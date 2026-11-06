package ru.tileclock;

public final class PlayerOrder {
    private PlayerOrder() { }
    public static int swap(String[] names,int[] faces,int first,int a,int b) {
        String name=names[a]; names[a]=names[b]; names[b]=name;
        int face=faces[a]; faces[a]=faces[b]; faces[b]=face;
        return first==a?b:first==b?a:first;
    }
}
