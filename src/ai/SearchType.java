package ai;

public enum SearchType {
    MINIMAX,
    ALPHABETA;

    public static SearchType[] getAllTypes(){
        return new SearchType[]{
                SearchType.MINIMAX,
                SearchType.ALPHABETA
        };
    }

    public static int amountOfTypes(){
        return getAllTypes().length;
    }
}
