package ru.tileclock;

public final class PlayerOrder {
    private PlayerOrder() { }
    /** Move a player while keeping everyone else's relative order. */
    public static void move(String[] names,int[] faces,int count,int from,int to) {
        if(count<2 || count>names.length || count>faces.length || from<0 || from>=count || to<0 || to>=count)
            throw new IllegalArgumentException("Invalid player move");
        String name=names[from]; int face=faces[from];
        int step=from<to?1:-1;
        for(int i=from;i!=to;i+=step) { names[i]=names[i+step]; faces[i]=faces[i+step]; }
        names[to]=name; faces[to]=face;
    }
    public static int swap(String[] names,int[] faces,int first,int a,int b) {
        String name=names[a]; names[a]=names[b]; names[b]=name;
        int face=faces[a]; faces[a]=faces[b]; faces[b]=face;
        return first==a?b:first==b?a:first;
    }
}
